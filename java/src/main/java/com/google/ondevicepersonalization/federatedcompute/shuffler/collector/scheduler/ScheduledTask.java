// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.ondevicepersonalization.federatedcompute.shuffler.collector.scheduler;

import com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.CollectorCore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The scheduled task. */
@Component
public class ScheduledTask {

  CollectorCore collector;

  public ScheduledTask(CollectorCore collector) {
    this.collector = collector;
  }

  @Scheduled(fixedDelay = 200)
  public void run() throws Exception {
    collector.processCollecting();
    collector.processAggregating();
  }

  @Scheduled(fixedDelay = 60000)
  public void runTimeouts() throws Exception {
    collector.processTimeouts();
  }
}
