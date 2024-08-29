"""Renames nodes within a TensorFlow graph so that the graph is more compact."""

import collections
from typing import Any, Callable, Collection, Iterable, Optional, Set

from absl import logging
from google.protobuf import text_format
import tensorflow as tf

SHORT_NAME_PREFIX = '.'
CONTROL_PREFIX = '^'
OUTPUT_MARKER = ':'
LOC_PREFIX = b'loc:@'
# The list of op nodes added by the `tf.Print` and `tf.print` APIs.
PRINT_OPS = frozenset(['Print', 'PrintV2'])
STRING_FORMAT_OPS = frozenset(['StringFormat'])


def _apply_fn_to_all_nodes_in_graphdef(
    graphdef: tf.compat.v1.GraphDef,
    node_fn: Callable[[tf.compat.v1.NodeDef], None],
) -> None:
  for node in graphdef.node:
    node_fn(node)
  # Also apply to nodes in the function library.
  for fn in graphdef.library.function:
    for node in fn.node_def:
      node_fn(node)


def _maybe_split_on_output_marker(input_str: str) -> tuple[str, str]:
  """Splits string on marker if exists."""
  marker_index = input_str.find(OUTPUT_MARKER)

  if marker_index >= 0:
    return input_str[:marker_index], input_str[marker_index:]
  else:
    return input_str, ''


def normalized_tensor_name(name: str) -> str:
  """Normalizes an input name, if necessary.

  This normalization handles control dependency prefixes (leading carats), and
  output suffixes (after the first colon).

  Examples:
    - `'^abc'` prefix for control dependencies
    - `'tensor:#'` for graph outputs
    - `'function:output:#'` for function graph outputs

  Args:
    name: The string name of the tensor from a GraphDef or FunctionDef.

  Returns:
    A string of the tensor name without any special suffies or prefixes, which
  can be used to refer to a node in the graph.
  """
  if name.startswith(CONTROL_PREFIX):
    return name[1:]
  return name.split(':', maxsplit=2)[0]


def filter_ops(
    node_defs: Iterable[tf.compat.v1.NodeDef],
    ops_to_remove: Collection[str],
    ops_to_conditionally_remove: Optional[Collection[str]] = None,
    conditional_ops: Optional[Collection[str]] = None,
    func_args: Optional[list[str]] = None,
) -> tuple[list[tf.compat.v1.NodeDef], list[tf.compat.v1.NodeDef]]:
  r"""Filters nodes that match ops from a list.

  Removes nodes that are ops from a particular set from a serialized tensorflow
  graph, adding control dependencies on the inputs of the removed nodes to the
  nodes that took the removed nodes as inputs. This ensured the order of
  evaluation of the graph nodes (the dependencies) is preserved after removing
  the nodes.

  An example graph with 'Keep' nodes and 'Remove' nodes (double edges are
  regular dependencies, single edges are control dependencies):

       Keep1    Keep2
         \\      /
          Remove1     Keep3
         /      \    //
    Keep4       Keep5
                 ||
               Remove2
                  |
                Keep6

  The result should be:

     Keep1  Keep2
       |  \/  |     Keep3
       |  /\  |   //
     Keep4  Keep5
              |
            Keep6

  In this new graph Keep4 and Keep5 both now have control dependencies on the
  Remove1's inputs (Keep1 and Keep2). Keep6 has also been updated to depend on
  Keep5 (inheriting Remove2's input as a control dependency).

  Args:
    node_defs: An iterable of `tf.compat.v1.NodeDef` proto messages.
    ops_to_remove: A set of string op names to remove from the graph.
    ops_to_conditionally_remove: A set of string op names to remove if they are
      the input to an op in `conditional_ops`.
    conditional_ops: `ops_to_conditionally_remove` are removed if they are
      inputs to these ops.
    func_args: An optional list of input names that are function args. Only used
      if `node_defs` are a list of nodes coming from a
      `function_pb2.FunctionDef` function graph.

  Returns:
    A `list` of `tf.compat.v1.NodeDef` that has no nodes that are ops in the
    `ops_to_remove` list.

  Raises:
    ValueError: If a node in the graph depends on the regular output (not a
    control dependency) of a removed node.
  """
  # A map of node name to its `tf.compat.v1.NodeDef` message. Used to quickly
  # look up any node in the graph by name.
  node_map = {}
  # A map of node name that is slated to be removed to the list of inputs
  # to that node. Must only contain names of nodes that are ops in
  # `ops_to_remove`. When rewriting, we must replace the removed nodes with
  # control dependenceis on their inputs to ensure the order of operations
  # remains correct.
  inputs_to_remap = {}
  # The list of `tf.compat.v1.NodeDef` in the original graph that we want to
  # retain, in the order they appeared in the original graph. Must not contain
  # any nodes where the `op` is a member of `ops_to_remove`.
  nodes_to_keep = []
  # The list of `tf.compat.v1.NodeDef` in the original graph that we want to
  # filter, in the order they appeared in the original graph. Only contains
  # nodes where the `op` is a member of `ops_to_remove`.
  nodes_to_filter = []

  # The list of `tf.compat.v1.NodeDef` input names to filter on. If the inputs
  # are of an op to conditionally remove, the node will be removed.
  inputs_to_filter_on = []

  if conditional_ops is None:
    conditional_ops = set([])

  if ops_to_conditionally_remove is None:
    ops_to_conditionally_remove = set([])

  if ops_to_conditionally_remove:
    for node in node_defs:
      if node.op in conditional_ops:
        inputs_to_filter_on.extend(
            [normalized_tensor_name(i) for i in node.input]
        )

    # The V2 print op duplicates an op that should be conditionally removed.
    # We must check that the conditional ops did not duplicate the node to be
    # removed, if it has been duplicated, we add to the node names to filter
    # out.
    for node in node_defs:
      if node.name in inputs_to_filter_on:
        if node.op in ops_to_conditionally_remove:
          inputs_to_filter_on.extend(
              [normalized_tensor_name(i) for i in node.input]
          )

  for node in node_defs:
    node_map[node.name] = node
    if node.op in ops_to_conditionally_remove:
      if node.name in inputs_to_filter_on:
        inputs_to_remap[node.name] = list(node.input)
        nodes_to_filter.append(node)
      else:
        nodes_to_keep.append(node)
    elif node.op in ops_to_remove:
      inputs_to_remap[node.name] = [i for i in node.input]
      nodes_to_filter.append(node)
    else:
      nodes_to_keep.append(node)

  # The list of nodes in the values of `inputs_to_remap` may contain nodes that
  # themsleves were removed (e.g. the value is also a key in the map). To make
  # node dependency replacement easier later, we precompute the transitive
  # inputs here.
  #
  # If we have an initial `inputs_to_remap` of:
  #   {
  #     Remove3: [Keep4],
  #     Remove2: [Keep3, Remove3],
  #     Remove1: [Remove2, Keep1, Keep2],
  #   }
  #
  # After this, we will have:
  #   {
  #     Remove3: [Keep4]
  #     Remove2: [Keep3, Keep4],
  #     Remove1: [Keep1, Keep2, Keep3, Keep4],
  #   }
  def get_clean_inputs(node_name):
    """Gets the list inputs, replacing nodes to remove with ancestors."""
    new_inputs = []
    for tensor_name in inputs_to_remap[node_name]:
      input_node_name = normalized_tensor_name(tensor_name)
      if func_args is not None and input_node_name in func_args:
        # node_in is a function argument, not a nodedef; we can skip.
        continue
      if node_map[input_node_name].op in ops_to_remove:
        # Note: we don't need to check here if node_in is a control dep because
        # it is also being removed.
        new_inputs.extend(get_clean_inputs(input_node_name))
      elif node_map[input_node_name].op in ops_to_conditionally_remove:
        if node_map[input_node_name].name in inputs_to_filter_on:
          new_inputs.extend(get_clean_inputs(input_node_name))
        else:
          new_inputs.append(input_node_name)
      else:
        new_inputs.append(input_node_name)
    return new_inputs

  for node_name in inputs_to_remap:
    inputs_to_remap[node_name] = get_clean_inputs(node_name)

  # Finally, walk over all the nodes we are going to keep and update all their
  # inputs based on the new input map.
  for node in nodes_to_keep:
    regular_inputs = []
    control_deps = []
    for input_name in node.input:
      normalized_input = normalized_tensor_name(input_name)
      if input_name.startswith(CONTROL_PREFIX):
        if normalized_input in inputs_to_remap:
          # Add the transitive inputs from the removed input as control deps.
          control_deps.extend(
              [CONTROL_PREFIX + n for n in inputs_to_remap[normalized_input]]
          )
        else:
          control_deps.append(input_name)
      else:
        if normalized_input in inputs_to_remap:
          raise ValueError(
              'Trying to filter ops {o} resulted in removing node that is used '
              'as input [{i}]. Only nodes with control dependencies can be '
              'removed'.format(o=ops_to_remove, i=input_name)
          )
        else:
          regular_inputs.append(input_name)
    # Control deps must come after regular inputs.
    node.input[:] = regular_inputs + control_deps
  return nodes_to_keep, nodes_to_filter


class CompactGraphOptions:
  """Contains options that control the behavior of compact_graph().

  Fields:

    find_names_in_class_attr: If true, then we will scan the _class attribute
        of nodes in order to find node names that should not be renamed.

    find_names_in_const_nodes: If true, then we will scan Const nodes that
        contain strings for more node names to avoid renaming.

    shorten_names: If true, then we will rename those nodes whose names are not
        important to a more compact form.  Nodes that are renamed have a name in
        the form ".XXX", where XXX is the hexadecimal index of the node.

    fold_constants: If true, then 'Const' nodes that have the same value will
        be combined into a single 'Const' node, and all references to
        duplicate 'Const' nodes will be rewritten to use a single node.

    remove_print_ops: If true, then print ops defined in `PRINT_OPS` will be
        removed from the graph.
  """

  def __init__(self, **kwargs):
    self.find_names_in_class_attr = True
    self.find_names_in_const_nodes = True
    self.shorten_names = True
    self.fold_constants = True
    self.remove_print_ops = True

    for k, v in kwargs.items():
      setattr(self, k, v)


def compact_graph(
    graph: tf.compat.v1.GraphDef,
    names_to_keep: list[str],
    options: CompactGraphOptions,
) -> None:
  """Compacts a TensorFlow graph by renaming interior nodes.

  Compacts a TensorFlow graph by renaming those nodes whose names are not
  important to a much more compact form.

  Args:
    graph: The input / output `tf.compat.v1.GraphDef`.  The graph is modified
      in-place.
    names_to_keep: A list of the names of graph nodes that should not be
      renamed. Nodes whose names are not found in this list could be renamed.
    options: Options which control which optimizations are performed. See
      CompactGraphOptions.

  Raises:
    Exception: Raised when the set of names to keep is empty.
  """
  assert isinstance(graph, tf.compat.v1.GraphDef)
  assert isinstance(names_to_keep, list)
  assert not options or isinstance(options, CompactGraphOptions)

  options = options or CompactGraphOptions()

  if not names_to_keep:
    raise Exception(
        'names_to_keep must contain at least one entry; otherwise, '
        'all nodes in the graph will be renamed, and it seems '
        'unlikely that your graph will be at all useful.'
    )

  # Convert the keep_names list to a set, in order to support efficient lookups
  # in the loops below. At the same time, remove any ":NN" output suffixes from
  # the node names.
  names_to_keep_set = set([_get_node_name(name) for name in names_to_keep])

  # Check to see whether there are any nodes that already have the short name
  # prefix.  We're not prepared to deal with naming conflicts.
  for node in graph.node:
    name = node.name
    if name.startswith(SHORT_NAME_PREFIX):
      raise Exception(
          'Node "%s" has forbidden prefix "%s".  '
          'Has this graph already been compacted?' % (name, SHORT_NAME_PREFIX)
      )

  # The _class attribute of some nodes contains references to node names.
  # We do not want to rename these node references, so we scan them and build a
  # set of names not to rename.
  names_to_keep_from_class_attr_set = set([])
  if options.find_names_in_class_attr:

    def find_names_to_keep(node: tf.compat.v1.NodeDef) -> None:
      if '_class' in node.attr:
        class_attr = node.attr['_class']
        for loc in class_attr.list.s:
          if not loc.startswith(LOC_PREFIX):
            raise Exception(
                'Node "%s" of type "%s" contains unrecognized "loc": %s'
                % (node.name, node.op, loc)
            )
          loc_name = loc[len(LOC_PREFIX) :]
          names_to_keep_from_class_attr_set.add(loc_name.decode('utf-8'))

    _apply_fn_to_all_nodes_in_graphdef(graph, find_names_to_keep)
    logging.debug(
        'Number of names found within node _class attributes: %d',
        len(names_to_keep_from_class_attr_set),
    )

  # Some Const nodes contain node names.  If we are not allowed to rename these
  # node references, then we must scan them and add them to the 'do not rename'
  # list. Note that this is "conservative", in the sense that this may prevent
  # some nodes from being renamed, when we actually could rename them, just due
  # to a naming coincidence.
  names_to_keep_from_consts = set([])
  if options.find_names_in_const_nodes:

    def find_names_to_keep_from_consts(node: tf.compat.v1.NodeDef) -> None:
      if node.op == 'Const':
        if 'value' in node.attr:
          value_attr = node.attr['value']
          tensor = value_attr.tensor
          for vi in range(0, len(tensor.string_val)):
            string_val = tensor.string_val[vi]
            names_to_keep_from_consts.add(string_val)
        else:
          logging.info("const node does not contain 'value' attr??")

    _apply_fn_to_all_nodes_in_graphdef(graph, find_names_to_keep_from_consts)
    logging.debug(
        'Number of names found within const nodes: %d',
        len(names_to_keep_from_consts),
    )

  names_to_keep_from_placeholders = set([])

  def find_names_to_keep_from_placeholders(node: tf.compat.v1.NodeDef) -> None:
    if node.op == 'Placeholder':
      names_to_keep_from_placeholders.add(node.name)

  _apply_fn_to_all_nodes_in_graphdef(
      graph, find_names_to_keep_from_placeholders
  )
  logging.debug(
      'Number of names found within placeholders nodes: %d',
      len(names_to_keep_from_placeholders),
  )
  # We won't shorten function names or function args (inputs and outputs) for
  # simplicity.
  names_to_keep_from_function_name_and_args = set([])
  for function in graph.library.function:
    names_to_keep_from_function_name_and_args.add(function.signature.name)
    function_args = [
        input_arg for input_arg in function.signature.input_arg
    ] + [output_arg for output_arg in function.signature.output_arg]
    for arg in function_args:
      names_to_keep_from_function_name_and_args.add(arg.name)

  if options.shorten_names:
    # Build a dict that maps names from old to new.
    name_remap = {}
    num_names_found_const = 0  # Number of names found in consts
    num_names_found_class_attr = 0  # Number of names found in class_attr

    def maybe_add_new_name_to_map(
        named_object: Any, index: int, object_type_str: str
    ) -> tuple[int, int]:
      name = (
          named_object if isinstance(named_object, str) else named_object.name
      )

      # Names might duplicate in function components but we don't want to
      # overwrite name maps.
      if name in name_remap:
        new_name = name_remap[name]
        logging.debug('renaming %s: %s --> %s', object_type_str, name, new_name)
        return 0, 0

      new_name = name
      found_class_attr = found_const = 0
      if name in names_to_keep_set:
        logging.debug(
            'not renaming %s (in keep set): %s', object_type_str, name
        )
      elif name in names_to_keep_from_class_attr_set:
        logging.debug(
            'not renaming %s (found in _class): %s', object_type_str, name
        )
        found_class_attr = 1
      elif name in names_to_keep_from_consts:
        logging.debug(
            'not renaming %s (found in const): %s', object_type_str, name
        )
        found_const = 0
      elif name in names_to_keep_from_placeholders:
        logging.debug(
            'not renaming %s (found in placeholder): %s', object_type_str, name
        )
      elif name in names_to_keep_from_function_name_and_args:
        logging.debug(
            'not renaming %s (found in function name or arg): %s',
            object_type_str,
            name,
        )
      else:
        new_name = SHORT_NAME_PREFIX + '%x' % index
        logging.debug('renaming %s: %s --> %s', object_type_str, name, new_name)
      name_remap[name] = new_name
      return found_class_attr, found_const

    for node in graph.node:
      found_class_attr, found_const = maybe_add_new_name_to_map(
          node, len(name_remap), 'node'
      )
      num_names_found_class_attr += found_class_attr
      num_names_found_const += found_const
    # Search through all named nodes and args in the function definitions.
    for function in graph.library.function:
      maybe_add_new_name_to_map(
          function.signature, len(name_remap), 'function component'
      )
      named_function_components = (
          list(function.node_def)
          + list(function.signature.input_arg)
          + list(function.signature.output_arg)
          + list(function.signature.control_output)
      )
      for component in named_function_components:
        found_class_attr, found_const = maybe_add_new_name_to_map(
            component, len(name_remap), 'function component'
        )
        num_names_found_class_attr += found_class_attr
        num_names_found_const += found_const

    logging.debug(
        'Number of nodes not renamed due to _class attr: %d',
        num_names_found_class_attr,
    )
    logging.debug(
        'Number of nodes not renamed due to Const nodes: %d',
        num_names_found_const,
    )

    # Make sure we found every entry in the keep_names list.
    # Note that we are intentionally checking against keep_names, and not
    # full_keep_names.
    for name in names_to_keep_set:
      if name not in name_remap:
        logging.warning('Node name "%s" was not found in the graph.', name)

    node_count = len(graph.node)
    logging.debug('node_count = %d', node_count)

    for node in graph.node:
      node_name = node.name
      node.name = name_remap[node_name]

      # Patch up input references.
      for j in range(len(node.input)):
        original_input_name = node.input[j]
        input_name = original_input_name

        # Decode the input spec and remap it.
        control = ''
        if input_name.startswith(CONTROL_PREFIX):
          input_name = input_name[len(CONTROL_PREFIX) :]
          control = CONTROL_PREFIX

        input_name, output_index_text = _maybe_split_on_output_marker(
            input_name
        )

        if input_name not in name_remap:
          raise Exception(
              'Node "%s" declares input "%s" which cannot be resolved.'
              % (node_name, original_input_name)
          )
        new_input_name = control + name_remap[input_name] + output_index_text
        node.input[j] = new_input_name

        logging.debug(
            '    input: %s --> %s', original_input_name, new_input_name
        )

    # Update names in function library.
    for function in graph.library.function:
      for node in function.node_def:
        node_name = node.name
        node.name = name_remap[node_name]

        # Patch up input references.
        for j in range(len(node.input)):
          original_input_name = node.input[j]
          input_name = original_input_name

          # Decode the input spec and remap it.
          control = ''
          if input_name.startswith(CONTROL_PREFIX):
            input_name = input_name[len(CONTROL_PREFIX) :]
            control = CONTROL_PREFIX

          input_name, output_index_text = _maybe_split_on_output_marker(
              input_name
          )

          if input_name not in name_remap:
            raise Exception(
                'Node "%s" declares input "%s" which cannot be resolved.'
                % (node_name, original_input_name)
            )
          new_input_name = control + name_remap[input_name] + output_index_text
          node.input[j] = new_input_name
          logging.debug(
              '    input: %s --> %s', original_input_name, new_input_name
          )

      def update_names_in_ret_map(ret_map: dict[str, str]) -> None:
        new_map = {}
        for key in ret_map:
          new_key = name_remap[key]
          value = ret_map[key]
          value_name, output_index_text = _maybe_split_on_output_marker(value)
          new_value = name_remap[value_name] + output_index_text
          new_map[new_key] = new_value
        ret_map.clear()
        for key, value in new_map.items():
          ret_map[key] = value

      update_names_in_ret_map(function.ret)
      update_names_in_ret_map(function.control_ret)
      function.signature.control_output[:] = sorted(function.control_ret.keys())

  # Constant Folding
  #
  # To fold constants, we find all 'Const' nodes, duplicate the node, clear the
  # 'name' field, re-serialize the NodeDef to a string, and use that string
  # to find other constants which are identical.  For each set of const nodes
  # that are identical (aside from 'name'), we create a new node with a name
  # '.kXX', where 'XX' is a hexadecimal number.  Then we scan all nodes, looking
  # for inputs that refer to any of the nodes in this equivalence set, and then
  # map them to the new node. Then we delete each of the nodes in the set.

  if options.fold_constants:
    # Find all of the unique const values in the graph, and for each unique
    # const value, find the list of node indices that have that value.
    # Also, skip nodes that have well-known names.
    #
    # The result of this loop is the unique_const_map. The keys of
    # unique_const_map are strings, which are the serialized form of a NodeDef
    # that contains the const value, with the 'name' field cleared. The values
    # of unique_const_map are instances of UniqueConst.
    unique_const_map = {}
    num_const_skipped = 0
    for node_index, node in enumerate(graph.node):
      if node.op != 'Const':
        continue

      # For now, we avoid touching const nodes that are in the keep set.
      # Note that we could be smarter, here. When we decide to skip a node, we
      # completely ignore it for the purpose of const folding. However, we
      # could still use such a constant for folding, so long as we don't modify
      # the node. Other duplicate const nodes could be deleted in favor of the
      # "kept" node. That would require a smarter algorithm, though, and for now
      # that doesn't seem justified.
      if node.name in names_to_keep_set:
        logging.debug('Skipping const node (is well known): %s', node.name)
        num_const_skipped += 1
        continue
      if node.name in names_to_keep_from_consts:
        logging.debug(
            'Skipping const node (mentioned in other constants): %s', node.name
        )
        num_const_skipped += 1
        continue
      if node.name in names_to_keep_from_class_attr_set:
        logging.debug('Skipping const node (in _class attr set): %s', node.name)
        num_const_skipped += 1
        continue

      # Build a NodeDef that is equivalent to the current node, but clear the
      # 'name' field. We then serialize this NodeDef, and we use this serialized
      # form as our const identity.
      unique_node_def = tf.compat.v1.NodeDef()
      unique_node_def.CopyFrom(node)
      unique_node_def.name = ''
      key = unique_node_def.SerializeToString()

      # Check unique_const_map to see if we've seen this const value before.
      # If we have not, then we add a new UniqueConst instance to the map,
      # and we store within it the NodeDef object (not serialized). (We will use
      # this NodeDef object below.) Then, regardless of whether this is the
      if key in unique_const_map:
        unique_entry = unique_const_map[key]
      else:
        unique_entry = UniqueConst(unique_node_def)
        unique_const_map[key] = unique_entry

      # Add the index of the current const node to the list of node indexes for
      # this unique const value.
      unique_entry.node_indexes.append(node_index)

    logging.debug('Finished scanning for const nodes.')
    logging.debug('Number of unique const nodes: %d', len(unique_const_map))

    next_const_id = 1

    # This contains a list of node indexes which we will delete.
    node_indexes_to_delete = []

    # Remember the size of the graph.node collection, because we're going to
    # modify it in the loop below.
    old_node_count = len(graph.node)

    # For each unique const value that has duplicates (there exists two or more
    # nodes with the same value):
    #
    #   * create a new node with this value.
    #   * remember all other nodes with this value, so we can delete them later.
    #   * rewrite all references to the existing nodes so that the reference
    #       points to the newly-created node.
    #
    # This loop reads and writes the Graph, by adding new nodes and by
    # modifying the Node.input list.
    for key, unique_const in unique_const_map.items():
      # Constant folding is only relevant if there's more than one node with
      # the same value.
      if len(unique_const.node_indexes) < 2:
        continue

      const_id = next_const_id
      next_const_id += 1

      # Build a set containing the node names of all of the const nodes in this
      # equivalency set.
      dup_const_names = set(
          [graph.node[index].name for index in unique_const.node_indexes]
      )

      # Create a new node for our constant.
      new_node_name = '.k%x' % const_id
      new_node_def = graph.node.add()
      new_node_def.CopyFrom(unique_const.node_def)
      new_node_def.name = new_node_name

      # Find all references (NodeDef.input entries) that refer to these consts,
      # and rewrite them so that they refer to our new constant node.
      # Note that because const nodes only define a single output (index 0),
      # and because the dependency on const nodes is never a control dependency,
      # we can just do direct string comparisons on 'input' values.
      for node in graph.node[:old_node_count]:
        for input_index in range(len(node.input)):
          if node.input[input_index] in dup_const_names:
            node.input[input_index] = new_node_name

      # Copy the list of node indexes for the const nodes to
      # node_indexes_to_delete.
      node_indexes_to_delete.extend(unique_const.node_indexes)

    # We're done rewriting references and creating new nodes.  Next we need to
    # delete the old constant nodes.  We have to do so carefully, because the
    # nodes are identified by index, and removing an entry from an array changes
    # the meaning of those indices.  So we sort the indices, then walk through
    # them backward.
    logging.debug(
        'Deleting old const nodes.  Num nodes to delete = %d',
        len(node_indexes_to_delete),
    )
    node_indexes_to_delete.sort(reverse=True)
    for node_index in node_indexes_to_delete:
      del graph.node[node_index]

    logging.debug('Constant folding done.')
    logging.debug('  Number of nodes before: %d', old_node_count)
    logging.debug('  Number of nodes after: %d', len(graph.node))
    if num_const_skipped > 0:
      logging.debug('  Number of nodes skipped: %d', num_const_skipped)


class UniqueConst:
  """Internal only. Used for implementation of const folding."""

  def __init__(self, unique_node_def: tf.compat.v1.NodeDef):
    self.node_def = unique_node_def
    self.node_indexes = []


def _get_node_name(name: str) -> str:
  """Returns TF node name, by removing the optional output suffix (:NN)."""
  ii = name.rfind(OUTPUT_MARKER)
  return name[:ii] if ii != -1 else name


class PruningGraphBuilder:
  """Builds a pruning graph from a collection of tf.compat.v1.GraphDefs.

  If compact is true, does fuzzy compaction while creating the graph.
  The compaction algorithm removes node names, input names, and attrs
  before comparing the remaining tf.compat.v1.NodeDefs by their serialized
  forms.
  """

  def __init__(self, compact: bool = True):
    self._compact = compact
    self._node_bytes_to_str = dict()
    self._pruning_graph = tf.compat.v1.GraphDef()
    # dictionary from a string version of a compacted node to a list of graph
    # names that use that op. This is for logging purposes when creating a
    # pruning graph.
    self._op_source_log = collections.defaultdict(set)

  def get_op_source_log(self) -> dict[str, Set[str]]:
    return self._op_source_log

  def _compact_node(self, node: tf.compat.v1.NodeDef) -> None:
    """Compacts a tensorflow.NodeDef in-place."""
    node_copy = tf.compat.v1.NodeDef()
    node_copy.CopyFrom(node)

    # Clear the node name, input, debug info, and certain attr fields. This
    # makes the graph invalid, but that doesn't currently affect pruning.
    node_copy.name = ''
    node_copy.ClearField('experimental_debug_info')
    del node_copy.input[:]
    if '_class' in node_copy.attr:
      # _class refers to specific named nodes, safe to drop for now.
      del node_copy.attr['_class']
    if node_copy.op == 'Const' and 'value' in node_copy.attr:
      del node_copy.attr['value']
    elif (
        node_copy.op in ['TextEncoder2', 'TextDecoderMulti']
        and 'text_encoder_config' in node_copy.attr
    ):
      del node_copy.attr['text_encoder_config']
    elif (
        node_copy.op == 'DictionaryLookup'
        and 'dictionary_description_proto' in node_copy.attr
    ):
      del node_copy.attr['dictionary_description_proto']
    return node_copy

  def _hashable_node_bytes(self, node: tf.compat.v1.NodeDef) -> str:
    """Returns a deterministically hashable string for the given NodeDef."""
    node_copy = tf.compat.v1.NodeDef()
    node_copy.CopyFrom(node)
    node_attr = sorted(
        [(k, node_copy.attr[k].SerializeToString()) for k in node_copy.attr]
    )
    # Because the NodeDef's attr field is a map, its serialization order is
    # non-deterministic. So we separately serialize its attrs from the rest
    # of it, returning an encoded byte string.
    node_copy.ClearField('attr')
    return node_copy.SerializeToString() + repr(node_attr).encode('UTF-8')

  def add_graph(
      self, source_graph: tf.compat.v1.GraphDef, graph_name: str = ''
  ) -> 'PruningGraphBuilder':
    """Extends the pruning graph with nodes from the given graph."""
    if self._compact:

      def _maybe_add_node(node):
        compact_node = self._compact_node(node)
        hashable_node_bytes = self._hashable_node_bytes(compact_node)
        compact_node_string = self._node_bytes_to_str.get(hashable_node_bytes)
        if compact_node_string is None:
          self._pruning_graph.node.extend([compact_node])
          compact_node_string = text_format.MessageToString(compact_node)  # pytype: disable=wrong-arg-types  # always-use-return-annotations
          compact_node_string = compact_node_string.replace('\n', ' ').strip()
          self._node_bytes_to_str[hashable_node_bytes] = compact_node_string
          self._op_source_log[compact_node_string].add(graph_name)
        else:
          self._op_source_log[compact_node_string].add(graph_name)

      for node in source_graph.node:
        _maybe_add_node(node)
      for function in source_graph.library.function:
        for node in function.node_def:
          _maybe_add_node(node)
    else:  # Just append
      self._pruning_graph.node.MergeFrom(source_graph.node)
      for function in source_graph.library.function:
        self._pruning_graph.node.MergeFrom(function.node_def)
    return self

  def build(self) -> tf.compat.v1.GraphDef:
    """Returns the assembled pruning graph."""
    pruning_graph = tf.compat.v1.GraphDef()
    pruning_graph.CopyFrom(self._pruning_graph)
    return pruning_graph
