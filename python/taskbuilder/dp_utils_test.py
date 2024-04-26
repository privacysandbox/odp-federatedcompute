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
import dp_utils


class DpUtilsTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._report_goal = 2000
    self._num_training_rounds = 1000
    self._dp_delta = 0.000001

  def test_noise_to_epsilon(self):
    small_noise = 1.0
    large_noise = 2.0

    large_epsilon = dp_utils.noise_to_epsilon(
        report_goal=self._report_goal,
        num_training_rounds=self._num_training_rounds,
        dp_delta=self._dp_delta,
        noise_multiplier=small_noise,
    )
    small_epsilon = dp_utils.noise_to_epsilon(
        report_goal=self._report_goal,
        num_training_rounds=self._num_training_rounds,
        dp_delta=self._dp_delta,
        noise_multiplier=large_noise,
    )
    self.assertLess(small_epsilon, large_epsilon)

  def test_epsilon_to_noise(self):
    small_epsilon = 4.0
    large_epsilon = 8.0

    large_noise = dp_utils.epislon_to_noise(
        report_goal=self._report_goal,
        num_training_rounds=self._num_training_rounds,
        dp_delta=self._dp_delta,
        dp_epsilon=small_epsilon,
    )
    small_noise = dp_utils.epislon_to_noise(
        report_goal=self._report_goal,
        num_training_rounds=self._num_training_rounds,
        dp_delta=self._dp_delta,
        dp_epsilon=large_epsilon,
    )
    self.assertGreater(large_noise, small_noise)


if __name__ == '__main__':
  absltest.main()
