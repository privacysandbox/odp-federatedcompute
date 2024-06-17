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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.scheduler;

import com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.core.TaskSchedulerCore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The scheduled task. */
@Component
public class ScheduledTask {

  TaskSchedulerCore taskSchedulerCore;

  public ScheduledTask(TaskSchedulerCore taskSchedulerCore) {
    this.taskSchedulerCore = taskSchedulerCore;
  }

  // TODO(b/295018999): Determine a good rate for the task.
  @Scheduled(fixedDelay = 200)
  public void run() throws Exception {
    taskSchedulerCore.processCreatedTasks();
    taskSchedulerCore.processActiveTasks();
  }

  @Scheduled(fixedDelay = 60000)
  public void processCompletedIterations() throws Exception {
    taskSchedulerCore.processCompletedIterations();
  }
}
