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


import copy
from absl.testing import absltest
import common
import config_validator
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2

TEST_DATA = task_builder_pb2.TaskConfig(
    mode=task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL,
    population_name='my_new_population',
    label_name='y',
    policies=task_builder_pb2.Policies(
        min_separation_policy=task_pb2.MinimumSeparationPolicy(
            minimum_separation=3
        ),
        data_availability_policy=task_pb2.DataAvailabilityPolicy(
            min_example_count=100
        ),
        model_release_policy=task_builder_pb2.ModelReleaseManagementPolicy(
            dp_target_epsilon=6,
            dp_delta=0.000001,
            num_max_training_rounds=1000,
        ),
    ),
    federated_learning=task_builder_pb2.FederatedLearning(
        learning_process=task_builder_pb2.LearningProcess(
            client_learning_rate=0.01,
            server_learning_rate=1.0,
            runtime_config=task_builder_pb2.RuntimeConfig(
                report_goal=2000, over_selection_rate=0.3
            ),
        ),
        evaluation=task_builder_pb2.Evaluation(
            source_training_population='source_population',
            checkpoint_selector='every_2_round',
            evaluation_traffic=0.1,
            report_goal=200,
            over_selection_rate=0.3,
        ),
    ),
    differential_privacy=task_builder_pb2.DifferentialPrivacy(
        noise_multiplier=0.1, clip_norm=0.1
    ),
)

POSITIVE_NUM_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} must be a positive number.'
)
PROBABILITY_ERROR_MSG = (
    '[Invalid task config]: key `{key_name}` has a bad value: `{value_name}` in'
    ' `{entity_name}`. {key_name} must be a probability.'
)
DP_DELTA_ERROR_MSG = (
    '[Invalid task config]: key `dp_delta` has a bad value: `{value_name}` in'
    ' `model_release_policy`. dp_delta must be a float number between 0'
    ' and 0.0001.'
)
DP_TARGET_EPSILON_ERROR_MSG = (
    '[Invalid task config]: key `dp_target_epsilon` has a bad value:'
    ' `{value_name}` in `model_release_policy`. dp_target_epsilon must be a'
    ' float number between 0 and 6.0.'
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
    self._test_data = copy.deepcopy(TEST_DATA)

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
        POSITIVE_NUM_ERROR_MSG.format(
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
        POSITIVE_NUM_ERROR_MSG.format(
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
        POSITIVE_NUM_ERROR_MSG.format(
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
        POSITIVE_NUM_ERROR_MSG.format(
            key_name='noise_multiplier',
            value_name=-0.1,
            entity_name='differential_privacy',
        ),
    ):
      config_validator.validate_metadata(self._test_data)

    self._test_data.differential_privacy.noise_multiplier = 0.1
    self._test_data.differential_privacy.clip_norm = -0.1
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        POSITIVE_NUM_ERROR_MSG.format(
            key_name='clip_norm',
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
        POSITIVE_NUM_ERROR_MSG.format(
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
        POSITIVE_NUM_ERROR_MSG.format(
            key_name='over_selection_rate',
            value_name=-0.1,
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
    self._test_data.policies.model_release_policy.dp_target_epsilon = 10.0
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        DP_TARGET_EPSILON_ERROR_MSG.format(value_name=10.0),
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

  def test_validate_fcp_dp(self):
    self._test_data.differential_privacy.noise_multiplier = 0.0
    dp_parameters = config_validator.validate_fcp_dp(
        task_config=self._test_data
    )
    self.assertEqual(0.1, dp_parameters.dp_clip_norm)
    self.assertGreater(dp_parameters.noise_multiplier, 0.0)


if __name__ == '__main__':
  absltest.main()
