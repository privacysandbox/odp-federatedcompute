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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.core;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters.TaskEntityConverter;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Implementation of TaskManagementCore interface. */
@Service
public class TaskManagementCoreImpl implements TaskManagementCore {

  private static final TaskInfo DEFAULT_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setTrainingInfo(TrainingInfo.getDefaultInstance())
          .build();

  private TaskDao taskDao;
  private BlobManager blobManager;

  public TaskManagementCoreImpl(TaskDao taskDao, BlobManager blobManager) {
    this.taskDao = taskDao;
    this.blobManager = blobManager;
  }

  public Optional<Task> getTaskById(String populationName, long taskId) {
    return taskDao
        .getTaskById(populationName, taskId)
        .map(
            taskEntity -> {
              Task task = TaskEntityConverter.TO_TASK.convert(taskEntity);
              return attachResourcePaths(task, taskEntity);
            });
  }

  public Task createTask(Task task) {
    Task.Builder inputBuilder = task.toBuilder();
    if (task.getStatus() == TaskStatus.Enum.UNKNOWN) {
      inputBuilder.setStatus(TaskStatus.Enum.CREATED);
    }

    // Set info with default value.
    if (!task.hasInfo()) {
      inputBuilder.setInfo(DEFAULT_TASK_INFO);
    }

    TaskEntity newTaskEntity = taskDao.createTask(convertToTaskEntityNoId(inputBuilder.build()));
    Task newTask = TaskEntityConverter.TO_TASK.convert(newTaskEntity);
    return attachResourcePaths(newTask, newTaskEntity);
  }

  private static TaskEntity convertToTaskEntityNoId(Task task) {
    return TaskEntityConverter.TO_TASK_ENTITY.convert(task).toBuilder().taskId(0).build();
  }

  private Task attachResourcePaths(Task task, TaskEntity taskEntity) {
    Task.Builder taskBuilder = task.toBuilder();
    BlobDescription[] devicePlanDescriptions =
        blobManager.generateUploadDevicePlanDescriptions(taskEntity);

    taskBuilder.addAllClientOnlyPlanUrl(
        Arrays.stream(devicePlanDescriptions)
            .map(BlobDescription::getUrl)
            .collect(toImmutableList()));

    BlobDescription[] serverPlanDescription =
        blobManager.generateUploadServerPlanDescription(taskEntity);
    taskBuilder.addAllServerPhaseUrl(
        Arrays.stream(serverPlanDescription)
            .map(BlobDescription::getUrl)
            .collect(toImmutableList()));

    IterationEntity baseIterationEntity =
        IterationEntity.builder()
            .populationName(taskEntity.getPopulationName())
            .taskId(taskEntity.getTaskId())
            .iterationId(0)
            .resultId(0)
            .build();

    BlobDescription[] initCheckpointDescription =
        blobManager.generateUploadCheckpointDescriptions(baseIterationEntity);
    taskBuilder.addAllInitCheckpointUrl(
        Arrays.stream(initCheckpointDescription)
            .map(BlobDescription::getUrl)
            .collect(toImmutableList()));

    BlobDescription[] metricsDescription =
        blobManager.generateUploadMetricsDescriptions(baseIterationEntity);
    taskBuilder.addAllMetricsUrl(
        Arrays.stream(metricsDescription).map(BlobDescription::getUrl).collect(toImmutableList()));
    return taskBuilder.build();
  }
}
