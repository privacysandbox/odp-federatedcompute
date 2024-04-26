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
import tensorflow as tf


class DatasetUtilsTest(absltest.TestCase):

  def test_parse_examples(self):
    examples = [
        self.create_example(('f1', ii), ('label', ii % 2)) for ii in range(3)
    ]
    serialized_example = [ex.SerializeToString() for ex in examples]
    decoder = {
        'f1': tf.io.FixedLenFeature(shape=(1,), dtype=tf.string),
        'label': tf.io.FixedLenFeature(shape=(1,), dtype=tf.string),
    }

    features, label = dataset_utils.parse_tf_example(
        serialized_example, decoder, 'label'
    )
    self.assertListEqual(
        list(features['f1']), list(tf.constant(['0', '1', '2']))
    )
    self.assertListEqual(list(label), list(tf.constant(['0', '1', '0'])))

  def create_example(
      self, feature1: Tuple[str, str], feature2: Tuple[str, str]
  ) -> tf.train.Example:
    """Creates a example with 2 features with only string as value type.

    For example, if inpuut is: feature1=('f1', 1), feature2 = ('label', 1),
    The output looks like this:
    features {
    feature {
        key: "f1"
        value {
        bytes_list {
            value: "1"
        }
        }
    }
    feature {
        key: "label"
        value {
        bytes_list {
            value: "1"
        }
        }
    }
    }
    Args:

    feature1: The first feature
    feature2: The second feature

    Returns:
    A tf.Example
    """
    key1 = str(feature1[0])
    key2 = str(feature2[0])
    value1 = [s.encode('utf-8') for s in str(feature1[1])]
    value2 = [s.encode('utf-8') for s in str(feature2[1])]
    return tf.train.Example(
        features=tf.train.Features(
            feature={
                key1: tf.train.Feature(
                    bytes_list=tf.train.BytesList(value=value1)
                ),
                key2: tf.train.Feature(
                    bytes_list=tf.train.BytesList(value=value2)
                ),
            }
        )
    )


if __name__ == '__main__':
  absltest.main()
