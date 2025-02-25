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


from absl.testing import absltest
import common
import config_validator
from shuffler.proto import task_builder_pb2
import test_utils

CLIP_NORM_FIXED_AGGREGATOR_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} is required and must be a positive float '
    'when using FIXED_GAUSSIAN or TREE_AGGREGATION DP Aggregator.'
)

CLIP_NORM_ADAPTIVE_AGGREGATOR_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} must be a positive float or be left empty '
    'when using ADAPTIVE_GAUSSIAN or ADAPTIVE_TREE DP Aggregator.'
)
POSITIVE_NUM_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} is required and must be a positive number.'
)
NONNEGATIVE_NUM_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} must be a nonnegative number.'
)
PROBABILITY_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} must be a probability.'
)
DP_DELTA_ERROR_MSG = (
    '[Invalid task config]: key `dp_delta` has a bad value: `{value_name}` in'
    ' `model_release_policy`. dp_delta must be a float number between 0'
    ' and 3.333333E-07'
)
DP_TARGET_EPSILON_ERROR_MSG = (
    '[Invalid task config]: key `dp_target_epsilon` has a bad value:'
    ' `{value_name}` in `model_release_policy`. dp_target_epsilon must be a'
    ' float number between 0 and 10.000000'
)
CHECKPOINT_SELECTOR_ERROR_MSG = (
    '[Invalid task config]: key `checkpoint_selector` has a bad value:'
    ' `{value_name}` in `evaluation`. checkpoint_selector must be in the format'
    ' of every_k_round or every_k_hour, where k is a positive integer.'
)
MISSING_REQUIRED_STRING_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} is required and must be set to an non-empty'
    ' string.'
)
REPORTING_GOAL_ERROR_MSG = (
    '[Invalid task config]: key `report_goal` has a bad value: `{value_name}`'
    ' in `{entity_name}`. report_goal is required and must be set to an integer'
    ' strictly greater than zero.'
)


class ConfigValidatorTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._test_data = test_utils.get_good_training_and_eval_task_config()

  def test_validate_good_training_and_eval_task(self):
    mode = config_validator.validate_metadata(self._test_data)
    self.assertEqual(task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL, mode)

  def test_validate_good_eval_only_task(self):
    self._test_data.mode = task_builder_pb2.TaskMode.Enum.EVAL_ONLY
    mode = config_validator.validate_metadata(self._test_data)
    self.assertEqual(task_builder_pb2.TaskMode.Enum.EVAL_ONLY, mode)

  def test_validate_good_training_only_task(self):
    self._test_data.mode = task_builder_pb2.TaskMode.Enum.TRAINING_ONLY
    mode = config_validator.validate_metadata(self._test_data)
    self.assertEqual(task_builder_pb2.TaskMode.Enum.TRAINING_ONLY, mode)

  def test_validate_required_string(self):
    self._test_data.population_name = ''
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        MISSING_REQUIRED_STRING_ERROR_MSG.format(
            key_name='population_name', value_name='', entity_name='task_config'
        ),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_positive_number_bad_value(self):
    self._test_data.policies.min_separation_policy.minimum_separation = -1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='minimum_separation',
            value_name=-1,
            entity_name='min_separation_policy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.policies.min_separation_policy.minimum_separation = 3
    self._test_data.policies.data_availability_policy.min_example_count = -1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='min_example_count',
            value_name=-1,
            entity_name='data_availability_policy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.policies.data_availability_policy.min_example_count = 100
    self._test_data.policies.model_release_policy.num_max_training_rounds = -1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='num_max_training_rounds',
            value_name=-1,
            entity_name='model_release_policy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.policies.model_release_policy.num_max_training_rounds = 1000
    self._test_data.differential_privacy.noise_multiplier = -0.1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='noise_multiplier',
            value_name=-0.1,
            entity_name='differential_privacy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.differential_privacy.clip_norm = 0.1
    self._test_data.federated_learning.learning_process.runtime_config.over_selection_rate = (
        -0.1
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='over_selection_rate',
            value_name=-0.1,
            entity_name='runtime_config',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.federated_learning.learning_process.runtime_config.over_selection_rate = (
        0.3
    )
    self._test_data.federated_learning.evaluation.over_selection_rate = -0.1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='over_selection_rate',
            value_name=-0.1,
            entity_name='evaluation',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.federated_learning.evaluation.over_selection_rate = 0.3
    self._test_data.policies.dataset_policy.batch_size = -1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        NONNEGATIVE_NUM_ERROR_MSG.format(
            key_name='batch_size',
            value_name=-1,
            entity_name='dataset_policy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.policies.dataset_policy.batch_size = 3
    self._test_data.federated_learning.evaluation.source_training_task_id = 0
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        POSITIVE_NUM_ERROR_MSG.format(
            key_name='source_training_task_id',
            value_name=0,
            entity_name='evaluation',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_probability_bad_value(self):
    self._test_data.federated_learning.learning_process.client_learning_rate = (
        -1.0
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        PROBABILITY_ERROR_MSG.format(
            key_name='client_learning_rate',
            value_name=-1.0,
            entity_name='learning_process',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.federated_learning.learning_process.client_learning_rate = (
        0.01
    )
    self._test_data.federated_learning.learning_process.server_learning_rate = (
        -1.0
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        PROBABILITY_ERROR_MSG.format(
            key_name='server_learning_rate',
            value_name=-1.0,
            entity_name='learning_process',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.federated_learning.learning_process.server_learning_rate = 1
    self._test_data.federated_learning.evaluation.evaluation_traffic = 2.0
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        PROBABILITY_ERROR_MSG.format(
            key_name='evaluation_traffic',
            value_name=2.0,
            entity_name='evaluation',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_dp_delta_bad_value(self):
    self._test_data.policies.model_release_policy.dp_delta = 0.001
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        DP_DELTA_ERROR_MSG.format(value_name=0.001),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_dp_target_bad_value(self):
    self._test_data.policies.model_release_policy.dp_target_epsilon = 12.0
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        DP_TARGET_EPSILON_ERROR_MSG.format(value_name=12.0),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_checkpoint_selector(self):
    self._test_data.federated_learning.evaluation.checkpoint_selector = (
        'every_*redacted*_round'
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        CHECKPOINT_SELECTOR_ERROR_MSG.format(
            value_name='every_*redacted*_round'
        ),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_report_goal(self):
    self._test_data.federated_learning.evaluation.report_goal = 0
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        REPORTING_GOAL_ERROR_MSG.format(
            value_name=0,
            entity_name='evaluation',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.federated_learning.evaluation.report_goal = 200
    self._test_data.federated_learning.learning_process.runtime_config.report_goal = (
        0
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        REPORTING_GOAL_ERROR_MSG.format(
            value_name=0,
            entity_name='runtime_config',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_clip_norm_bad_value(self):
    self._test_data.differential_privacy.clip_norm = 0.0
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        CLIP_NORM_FIXED_AGGREGATOR_ERROR_MSG.format(
            key_name='clip_norm',
            value_name=0.0,
            entity_name='differential_privacy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.differential_privacy.init_clip_norm = -1.0
    self._test_data.differential_privacy.type = (
        task_builder_pb2.DpAggregator.ADAPTIVE_GAUSSIAN
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        CLIP_NORM_ADAPTIVE_AGGREGATOR_ERROR_MSG.format(
            key_name='init_clip_norm',
            value_name=-1.0,
            entity_name='differential_privacy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

  def test_validate_fcp_dp(self):
    dp_parameters = config_validator.validate_fcp_dp(
        task_config=self._test_data, flags=task_builder_pb2.ExperimentFlags()
    )
    self.assertEqual(0.1, dp_parameters.dp_clip_norm)
    self.assertEqual(6.0, dp_parameters.noise_multiplier)
    self.assertEqual(0.000000001, dp_parameters.dp_delta)
    self.assertLess(dp_parameters.dp_epsilon, 0.2)
    self.assertEqual(1000, dp_parameters.num_training_rounds)

  def test_validate_fcp_dp_calibration(self):
    self._test_data.differential_privacy.noise_multiplier = 0.0
    dp_parameters = config_validator.validate_fcp_dp(
        task_config=self._test_data, flags=task_builder_pb2.ExperimentFlags()
    )
    self.assertEqual(0.1, dp_parameters.dp_clip_norm)
    self.assertGreater(dp_parameters.noise_multiplier, 0.0)
    self.assertEqual(0.000000001, dp_parameters.dp_delta)
    self.assertEqual(6.0, dp_parameters.dp_epsilon)
    self.assertEqual(1000, dp_parameters.num_training_rounds)

  def test_validate_fcp_dp_calibration_init_clip_norm(self):
    self._test_data.differential_privacy.init_clip_norm = 0.0
    self._test_data.differential_privacy.type = (
        task_builder_pb2.DpAggregator.ADAPTIVE_GAUSSIAN
    )
    dp_parameters = config_validator.validate_fcp_dp(
        task_config=self._test_data, flags=task_builder_pb2.ExperimentFlags()
    )
    self.assertEqual(0.1, dp_parameters.dp_clip_norm)


if __name__ == '__main__':
  absltest.main()
