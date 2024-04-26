// Copyright 2024 Google LLC
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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core;

import com.google.internal.federatedcompute.v1.Resource;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityPolicyEvalSpec;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityTaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.RandomGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A helper class for TaskAssignmentCore. */
@Component
public class TaskAssignmentCoreHelper {

  @Autowired private RandomGenerator randomGenerator;
  @Autowired private BlobManager blobManager;

  public static EligibilityTaskInfo createEligibilityTaskInfo(
      long currentIterationId, TaskEntity taskEntity) {
    TaskInfo taskInfo = taskEntity.getProtoInfo();

    if (!taskInfo.hasTrainingInfo()) {
      return EligibilityTaskInfo.getDefaultInstance();
    }

    return EligibilityTaskInfo.newBuilder()
        .addAllEligibilityPolicies(
            taskInfo
                .getTrainingInfo()
                .getEligibilityTaskInfo()
                .getEligibilityPoliciesList()
                .stream()
                .map(policy -> loadIterationId(policy, currentIterationId))
                .toList())
        .build();
  }

  private static EligibilityPolicyEvalSpec loadIterationId(
      EligibilityPolicyEvalSpec spec, long currentIterationId) {
    if (spec.hasMinSepPolicy()) {
      return spec.toBuilder()
          .setMinSepPolicy(spec.getMinSepPolicy().toBuilder().setCurrentIndex(currentIterationId))
          .build();
    }
    return spec;
  }

  /**
   * Selects a TaskEntity from the provided set, with selection probability weighted by each task's
   * traffic weight. The traffic weight should be a whole number from 1 to 1000.
   *
   * @param taskEntitySet A set containing TaskEntity objects.
   * @return An Optional containing the selected TaskEntity, or Optional.empty() if the input set is
   *     empty or contains only tasks with zero weight.
   * @throws IllegalArgumentException if any TaskEntity in the set has a null 'info' field or a null
   *     'trafficWeight' field.
   */
  public Optional<TaskEntity> selectTask(Set<TaskEntity> taskEntitySet)
      throws IllegalStateException {
    if (taskEntitySet.isEmpty()) {
      return Optional.empty();
    }

    if (taskEntitySet.size() == 1) {
      return Optional.of(taskEntitySet.iterator().next());
    }

    long totalWeight = 0;
    TreeMap<Long, TaskEntity> taskEntityByAccumulatedWeight = new TreeMap<>();
    for (TaskEntity entity : taskEntitySet) {
      TaskInfo info = ProtoParser.toProto(entity.getInfo(), TaskInfo.getDefaultInstance());
      totalWeight += info.getTrafficWeight();
      taskEntityByAccumulatedWeight.put(totalWeight, entity);
    }

    long rand = randomGenerator.nextLong(totalWeight) + 1;
    TaskEntity targetTaskEntity = taskEntityByAccumulatedWeight.ceilingEntry(rand).getValue();
    return Optional.of(targetTaskEntity);
  }

  /**
   * Creates an Optional containing a TaskAssignment object based on the provided task, iteration,
   * and assignment entities. Handles the necessary conversions and the building of the
   * TaskAssignment object.
   *
   * @param taskEntity The TaskEntity representing the task details.
   * @param assignmentEntity The AssignmentEntity representing the assignment details.
   * @return An Optional containing the created TaskAssignment object, or an empty Optional if
   *     creation fails.
   * @throws IllegalArgumentException If any of the input entities are null.
   */
  public Optional<TaskAssignment> createTaskAssignment(
      TaskEntity taskEntity, AssignmentEntity assignmentEntity) {
    EligibilityTaskInfo eligibilityTaskInfo =
        createEligibilityTaskInfo(assignmentEntity.getIterationId(), taskEntity);
    IterationId deviceCheckpointIterationId =
        IterationId.builder()
            .populationName(assignmentEntity.getPopulationName())
            .taskId(assignmentEntity.getTaskId())
            .iterationId(assignmentEntity.getBaseOnResultId())
            .build();
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName(assignmentEntity.getPopulationName())
            .taskId(assignmentEntity.getTaskId())
            .iterationId(assignmentEntity.getIterationId())
            .attemptId(assignmentEntity.getAttemptId())
            .assignmentId(assignmentEntity.getSessionId())
            .build();

    TaskAssignment.Builder builder =
        TaskAssignment.newBuilder()
            .setPopulationName(assignmentEntity.getPopulationName())
            .setTaskId(String.valueOf(assignmentEntity.getTaskId()))
            .setAggregationId(String.valueOf(assignmentEntity.getIterationId()))
            .setAssignmentId(assignmentEntity.getSessionId())
            .setTaskName(
                createTaskName(assignmentEntity.getPopulationName(), assignmentEntity.getTaskId()))
            .setInitCheckpoint(
                convert(
                    blobManager.generateDownloadCheckpointDescription(
                        assignmentId, deviceCheckpointIterationId)))
            .setPlan(convert(blobManager.generateDownloadDevicePlanDescription(assignmentEntity)))
            .setSelfUri(createAssignmentUri(assignmentEntity))
            .setEligibilityTaskInfo(eligibilityTaskInfo);

    return Optional.of(builder.build());
  }

  private static String createTaskName(String populationName, long taskId) {
    return "/population/" + populationName + "/task/" + taskId;
  }

  private static Resource convert(BlobDescription blobDescription) {
    return Resource.newBuilder().setUri(blobDescription.getUrl()).build();
  }

  private static String createAssignmentUri(AssignmentEntity entity) {
    return "/population/"
        + entity.getPopulationName()
        + "/task/"
        + entity.getTaskId()
        + "/aggregation/"
        + entity.getIterationId()
        + "/task-assignment/"
        + entity.getSessionId();
  }
}
