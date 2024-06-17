# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
import common
import dp_utils
from shuffler.proto import task_builder_pb2


def validate_metadata(
    task_config: task_builder_pb2.TaskConfig,
) -> task_builder_pb2.TaskMode.Enum:
  """Perform validations on task config, such as check on bad value.

  Args:
    task_config: the adopter-provided task configurations.

  Returns:
    The task mode if `task_config` is valid.

  Raises:
    TaskBuilderException: if any validation check fails.
  """
  # Population name must be set to a non-empty string
  _validate_non_empty_string(
      key_name='population_name',
      value=task_config.population_name,
      entity_name='task_config',
  )

  # Validate `policies` setup
  _validate_policy_setup(task_config.policies)

  # Validate `federated_learning` setup
  _validate_federated_learning_setup(
      task_mode=task_config.mode, fl_setup=task_config.federated_learning
  )

  # Validate `differential_privacy` setup
  _validate_differential_privacy_setup(task_config.differential_privacy)
  return task_config.mode


def validate_fcp_dp(
    task_config: task_builder_pb2.TaskConfig,
    flags: task_builder_pb2.ExperimentFlags,
) -> common.DpParameter:
  """Prepare DP parameters and validate DP accounting for training task.

  Args:
      task_config: the adopter-provided task configurations.

  Returns:
      DP parameters that build dp-noised learning process.

  Raises:
      TaskBuilderException: if the DP accounting validation fails.
  """
  training_report_goal = (
      task_config.federated_learning.learning_process.runtime_config.report_goal
  )
  dp_setup = task_config.differential_privacy
  model_release_policy = task_config.policies.model_release_policy
  dp_clip_norm = dp_setup.clip_norm
  noise_multiplier = dp_setup.noise_multiplier

  dp_target_epsilon = model_release_policy.dp_target_epsilon
  dp_delta = model_release_policy.dp_delta
  if not dp_delta:
    dp_delta = common.DEFAULT_DP_DELTA
    logging.info(f'`dp_delta` is set to default value: {dp_delta}')
  if not dp_target_epsilon:
    dp_target_epsilon = common.DEFAULT_DP_EPSILON
    logging.info(
        f'`dp_target_epsilon` is set to default value: {dp_target_epsilon}'
    )
  num_training_rounds = model_release_policy.num_max_training_rounds

  # calibrate noise if not provided
  if not noise_multiplier:
    noise_multiplier = dp_utils.epislon_to_noise(
        report_goal=training_report_goal,
        num_training_rounds=num_training_rounds,
        dp_delta=dp_delta,
        dp_epsilon=dp_target_epsilon,
    )
  else:
    dp_epsilon = dp_utils.noise_to_epsilon(
        report_goal=training_report_goal,
        num_training_rounds=num_training_rounds,
        dp_delta=dp_delta,
        noise_multiplier=noise_multiplier,
    )
    if not flags.skip_dp_check and dp_epsilon > dp_target_epsilon:
      raise common.TaskBuilderException(
          common.CONFIG_VALIDATOR_ERROR_PREFIX
          + common.DP_ACCOUNTING_CHECK_ERROR_MSG
      )

  # DP configs for building learning process
  return common.DpParameter(
      noise_multiplier=noise_multiplier,
      dp_clip_norm=dp_clip_norm,
  )


def _validate_policy_setup(policy_setup: task_builder_pb2.Policies):
  """Perform validations on policies, such as check on bad value.

  Args:
    policy_setup: the `Policies` setup from task configurations.

  Raises:
    TaskBuilderException: if any validation check fails.
  """
  # minimum separation
  _validate_positive_number(
      key_name='minimum_separation',
      number=policy_setup.min_separation_policy.minimum_separation,
      entity_name='min_separation_policy',
  )

  # minimum example count
  _validate_positive_number(
      key_name='min_example_count',
      number=policy_setup.data_availability_policy.min_example_count,
      entity_name='data_availability_policy',
  )

  model_release_policy = policy_setup.model_release_policy

  # dp target epsilon
  _validate_dp_target(model_release_policy.dp_target_epsilon)

  # dp delta
  _validate_dp_delta(model_release_policy.dp_delta)

  # number of training rounds
  _validate_positive_number(
      key_name='num_max_training_rounds',
      number=model_release_policy.num_max_training_rounds,
      entity_name='model_release_policy',
  )

  # dataset preprocessing policies
  dataset_policy = policy_setup.dataset_policy
  _validate_positive_number(
      key_name='batch_size',
      number=dataset_policy.batch_size,
      entity_name='dataset_policy',
  )


def _validate_federated_learning_setup(
    task_mode: task_builder_pb2.TaskMode.Enum,
    fl_setup: task_builder_pb2.FederatedLearning,
):
  """Perform validations on federated learning, such as check on bad value.

  Args:
    fl_setup: the `FederatedLearning` setup from task configurations.

  Returns:
    The task mode if `fl_setup` is valid.

  Raises:
    TaskBuilderException: if any validation check fails.
  """
  learning_process = fl_setup.learning_process

  # client learning rate
  _validate_probability(
      key_name='client_learning_rate',
      number=learning_process.client_learning_rate,
      entity_name='learning_process',
  )

  # server learning rate
  _validate_probability(
      key_name='server_learning_rate',
      number=learning_process.server_learning_rate,
      entity_name='learning_process',
  )

  # training task reporting goal
  _validate_report_goal(
      report_goal=learning_process.runtime_config.report_goal,
      entity_name='runtime_config',
  )

  # training over selection rate
  _validate_positive_number(
      key_name='over_selection_rate',
      number=learning_process.runtime_config.over_selection_rate,
      entity_name='runtime_config',
  )

  # check evaluation info
  if task_mode != task_builder_pb2.TaskMode.Enum.TRAINING_ONLY:
    evalution_info = fl_setup.evaluation

    # eval task reporting goal
    _validate_report_goal(
        report_goal=evalution_info.report_goal, entity_name='evaluation'
    )

    # eval over selection rate
    _validate_positive_number(
        key_name='over_selection_rate',
        number=evalution_info.over_selection_rate,
        entity_name='evaluation',
    )

    # checkpoint selector
    _validate_checkpoint_selector(evalution_info.checkpoint_selector)

    # evaluation traffic
    _validate_probability(
        key_name='evaluation_traffic',
        number=evalution_info.evaluation_traffic,
        entity_name='evaluation',
    )


def _validate_differential_privacy_setup(
    dp_setup: task_builder_pb2.DifferentialPrivacy,
):
  """Perform validations on differential privacy, such as check on bad value.

  Args:
    dp_setup: the `DifferentialPrivacy` setup from task configurations.

  Raises:
    TaskBuilderException: if any validation check fails.
  """
  _validate_positive_number(
      key_name='noise_multiplier',
      number=dp_setup.noise_multiplier,
      entity_name='differential_privacy',
  )
  _validate_positive_number(
      key_name='clip_norm',
      number=dp_setup.clip_norm,
      entity_name='differential_privacy',
  )


def _validate_positive_number(
    key_name: str, number: int | float, entity_name: str
):
  """Validate the value of `key_name` is a positive number."""
  if number < 0:
    raise common.TaskBuilderException(
        common.CONFIG_VALIDATOR_ERROR_PREFIX
        + common.BAD_VALUE_ERROR_MSG.format(
            key_name=key_name,
            value_name=number,
            entity_name=entity_name,
            debug_msg=key_name + ' must be a positive number.',
        )
    )


def _validate_dp_delta(dp_delta: float):
  """Validate the value of `dp_delta` is in legitimate range.

  Since user-level DP is enforced, `dp_delta` must be a probability
  less than `1/n`, where `n` is the number of estimated total devices.

  We set `n` to 10000 by default.

  Args:
    dp_delta: the value of DP delta.
  """
  if dp_delta < 0.0 or dp_delta > common.DEFAULT_DP_DELTA:
    raise common.TaskBuilderException(
        common.CONFIG_VALIDATOR_ERROR_PREFIX
        + common.BAD_VALUE_ERROR_MSG.format(
            key_name='dp_delta',
            value_name=dp_delta,
            entity_name='model_release_policy',
            debug_msg='dp_delta must be a float number between 0 and %E'
            % common.DEFAULT_DP_DELTA,
        )
    )


def _validate_dp_target(dp_target_epsilon: float):
  """Validate the value of `dp_target` is in legitimate range.

  Range: (0.0, 6.0), where 6.0 is the system-provided target epsilon

  Args:
    dp_target_epsilon: the value of DP targeting epsilon.
  """
  if dp_target_epsilon < 0.0 or dp_target_epsilon > common.DEFAULT_DP_EPSILON:
    raise common.TaskBuilderException(
        common.CONFIG_VALIDATOR_ERROR_PREFIX
        + common.BAD_VALUE_ERROR_MSG.format(
            key_name='dp_target_epsilon',
            value_name=dp_target_epsilon,
            entity_name='model_release_policy',
            debug_msg=(
                'dp_target_epsilon must be a float number between 0 and %f'
                % common.DEFAULT_DP_EPSILON
            ),
        )
    )


def _validate_probability(key_name: str, number: float, entity_name: str):
  """Validate the value of `key_name` is a probability (float number from 0.0 to 1.0)."""
  if number < 0.0 or number > 1.0:
    raise common.TaskBuilderException(
        common.CONFIG_VALIDATOR_ERROR_PREFIX
        + common.BAD_VALUE_ERROR_MSG.format(
            key_name=key_name,
            value_name=number,
            entity_name=entity_name,
            debug_msg=key_name + ' must be a probability.',
        )
    )


def _validate_checkpoint_selector(checkpoint_selector: str):
  """Validate the value of `checkpoint_selector` is correctly formatted as `every_k_{round|hour}`."""
  if not checkpoint_selector:
    return
  values = checkpoint_selector.split('_')
  if (
      len(values) == 3
      and values[0] == 'every'
      and values[2] in ['round', 'hour']
  ):
    k = values[1]
    if k.isnumeric():
      return
  raise common.TaskBuilderException(
      common.CONFIG_VALIDATOR_ERROR_PREFIX
      + common.BAD_VALUE_ERROR_MSG.format(
          key_name='checkpoint_selector',
          value_name=checkpoint_selector,
          entity_name='evaluation',
          debug_msg=(
              'checkpoint_selector must be in the format of every_k_round or'
              ' every_k_hour, where k is a positive integer.'
          ),
      )
  )


def _validate_non_empty_string(key_name: str, value: str, entity_name: str):
  if not value:
    raise common.TaskBuilderException(
        common.CONFIG_VALIDATOR_ERROR_PREFIX
        + common.BAD_VALUE_ERROR_MSG.format(
            key_name=key_name,
            value_name=value,
            entity_name=entity_name,
            debug_msg=key_name
            + ' is required and must be set to an non-empty string.',
        )
    )


def _validate_report_goal(report_goal: int, entity_name: str):
  if report_goal <= 0:
    raise common.TaskBuilderException(
        common.CONFIG_VALIDATOR_ERROR_PREFIX
        + common.BAD_VALUE_ERROR_MSG.format(
            key_name='report_goal',
            value_name=report_goal,
            entity_name=entity_name,
            debug_msg=(
                'report_goal is required and must be set to an integer strictly'
                ' greater than zero.'
            ),
        )
    )
