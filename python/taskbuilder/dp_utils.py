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

import functools
import common
from dp_accounting import dp_event
from dp_accounting import mechanism_calibration
from dp_accounting import pld


def noise_to_epsilon(
    report_goal: int,
    num_training_rounds: int,
    dp_delta: float,
    noise_multiplier: float,
) -> float:
  """Calculate DP epsilon given a noise multiplier."""
  event = _make_dp_event(noise_multiplier, report_goal, num_training_rounds)
  accountant = _make_dp_accountant()
  accountant.compose(event)
  return accountant.get_epsilon(dp_delta)


def epislon_to_noise(
    report_goal: int,
    num_training_rounds: int,
    dp_delta: float,
    dp_epsilon: float,
) -> float:
  """Calibrate the minimal noise that guarantees the provided privacy budget."""
  make_event = functools.partial(
      _make_dp_event,
      report_goal=report_goal,
      num_training_rounds=num_training_rounds,
  )
  return mechanism_calibration.calibrate_dp_mechanism(
      make_fresh_accountant=_make_dp_accountant,
      make_event_from_param=make_event,
      target_epsilon=dp_epsilon,
      target_delta=dp_delta,
  )


def _make_dp_event(
    noise: float, report_goal: int, num_training_rounds: int
) -> dp_event.DpEvent:
  if num_training_rounds == 0:
    return dp_event.NoOpDpEvent()
  sampling_probability = report_goal / common.DEFAULT_TOTAL_POPULATION
  return dp_event.SelfComposedDpEvent(
      dp_event.PoissonSampledDpEvent(
          sampling_probability, dp_event.GaussianDpEvent(noise)
      ),
      num_training_rounds,
  )


def _make_dp_accountant() -> pld.PLDAccountant:
  return pld.PLDAccountant()
