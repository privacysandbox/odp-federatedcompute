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

from typing import Tuple
from absl.testing import absltest
import dataset_utils
from shuffler.proto import task_builder_pb2
import tensorflow as tf
import test_utils


class DatasetUtilsTest(absltest.TestCase):

  def create_example(
      self, f1_value: str, f2_value: int, label_value: str
  ) -> tf.train.Example:
    """Creates a example with 2 features with only string as value type.

    Returns:
    A tf.Example
    """
    return tf.train.Example(
        features=tf.train.Features(
            feature={
                'f1': tf.train.Feature(
                    bytes_list=tf.train.BytesList(
                        value=[f1_value.encode('utf-8')]
                    )
                ),
                'f2': tf.train.Feature(
                    int64_list=tf.train.Int64List(value=[f2_value])
                ),
                'label': tf.train.Feature(
                    bytes_list=tf.train.BytesList(
                        value=[label_value.encode('utf-8')]
                    )
                ),
            }
        )
    )

  def test_parse_examples(self):
    examples = [
        self.create_example(str(ii), ii, str(ii % 2)) for ii in range(3)
    ]
    serialized_example = [ex.SerializeToString() for ex in examples]
    decoder = {
        'f1': tf.io.FixedLenFeature(shape=(1), dtype=tf.string),
        'f2': tf.io.FixedLenFeature(shape=(1), dtype=tf.int64),
        'label': tf.io.FixedLenFeature(shape=(1), dtype=tf.string),
    }
    is_dtype_int32 = {'f1': False, 'f2': True, 'label': False}

    features, label = dataset_utils.parse_tf_example(
        serialized_example, decoder, 'label', is_dtype_int32
    )
    self.assertListEqual(
        list(features['f1']), list(tf.constant(['0', '1', '2']))
    )
    self.assertListEqual(list(label), list(tf.constant(['0', '1', '0'])))

  def test_get_data_specs_training_and_eval(self):
    task_config = test_utils.get_good_training_and_eval_task_config()
    training_data_spec, eval_data_spec = dataset_utils.get_data_specs(
        task_config=task_config, preprocessing_fn=None
    )
    self.assertEqual(
        'training_collection',
        training_data_spec.example_selector_proto.collection_uri,
    )
    self.assertEqual(
        'eval_collection', eval_data_spec.example_selector_proto.collection_uri
    )

  def test_get_data_specs_training_only(self):
    task_config = test_utils.get_good_training_and_eval_task_config()
    task_config.mode = task_builder_pb2.TaskMode.Enum.TRAINING_ONLY
    training_data_spec, missing_eval = dataset_utils.get_data_specs(
        task_config=task_config, preprocessing_fn=None
    )
    self.assertEqual(
        'training_collection',
        training_data_spec.example_selector_proto.collection_uri,
    )
    self.assertIsNone(missing_eval)

  def test_get_data_specs_eval_only(self):
    task_config = test_utils.get_good_training_and_eval_task_config()
    task_config.mode = task_builder_pb2.TaskMode.Enum.EVAL_ONLY
    eval_data_spec, missing_training = dataset_utils.get_data_specs(
        task_config=task_config, preprocessing_fn=None
    )
    self.assertEqual(
        'eval_collection', eval_data_spec.example_selector_proto.collection_uri
    )
    self.assertIsNone(missing_training)


if __name__ == '__main__':
  absltest.main()
