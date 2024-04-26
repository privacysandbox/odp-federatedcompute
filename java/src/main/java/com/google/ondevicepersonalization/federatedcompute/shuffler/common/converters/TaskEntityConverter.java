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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters;

import com.google.common.base.Converter;
import com.google.common.base.Strings;
import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;

/** Converter between {@link TaskEntity} and {@link Task}. */
public final class TaskEntityConverter extends Converter<TaskEntity, Task> {

  private TaskEntityConverter() {}

  public static final Converter<TaskEntity, Task> TO_TASK = new TaskEntityConverter();
  public static final Converter<Task, TaskEntity> TO_TASK_ENTITY = TO_TASK.reverse();

  @Override
  protected Task doForward(TaskEntity entity) {
    Task.Builder builder =
        Task.newBuilder()
            .setPopulationName(entity.getPopulationName())
            .setTaskId(entity.getTaskId())
            .setTotalIteration(entity.getTotalIteration())
            .setMinAggregationSize(entity.getMinAggregationSize())
            .setMaxAggregationSize(entity.getMaxAggregationSize())
            .setStatus(TaskEntityStatusConverter.TO_TASK_STATUS.convert(entity.getStatus()))
            .setMaxParallel(entity.getMaxParallel())
            .setCorrelationId(entity.getCorrelationId())
            .setMinClientVersion(entity.getMinClientVersion())
            .setMaxClientVersion(entity.getMaxClientVersion());

    // StartTaskNoEarlierThan is currently unsupported.
    if (entity.getStartTaskNoEarlierThan() != null) {
      builder.setStartTaskNoEarlierThan(
          TimestampInstantConverter.TO_TIMESTAMP.convert(entity.getStartTaskNoEarlierThan()));
    }
    if (entity.getDoNotCreateIterationAfter() != null) {
      builder.setDoNotCreateIterationAfter(
          TimestampInstantConverter.TO_TIMESTAMP.convert(entity.getDoNotCreateIterationAfter()));
    }
    if (entity.getStartedTime() != null) {
      builder.setStartedTime(
          TimestampInstantConverter.TO_TIMESTAMP.convert(entity.getStartedTime()));
    }
    if (entity.getStopTime() != null) {
      builder.setStopTime(TimestampInstantConverter.TO_TIMESTAMP.convert(entity.getStopTime()));
    }
    if (entity.getCreatedTime() != null) {
      builder.setCreatedTime(
          TimestampInstantConverter.TO_TIMESTAMP.convert(entity.getCreatedTime()));
    }
    if (!Strings.isNullOrEmpty(entity.getInfo())) {
      builder.setInfo(ProtoParser.toProto(entity.getInfo(), TaskInfo.getDefaultInstance()));
    }

    return builder.build();
  }

  @Override
  protected TaskEntity doBackward(Task task) {
    TaskEntity.TaskEntityBuilder builder =
        TaskEntity.builder()
            .populationName(task.getPopulationName())
            .taskId(task.getTaskId())
            .totalIteration(task.getTotalIteration())
            .minAggregationSize(task.getMinAggregationSize())
            .maxAggregationSize(task.getMaxAggregationSize())
            .status(TaskEntityStatusConverter.TO_TASK_ENTITY_STATUS.convert(task.getStatus()))
            .maxParallel(task.getMaxParallel())
            .correlationId(task.getCorrelationId())
            .minClientVersion(task.getMinClientVersion())
            .maxClientVersion(task.getMaxClientVersion());

    // StartTaskNoEarlier is currenlt unsupported.
    if (task.hasStartTaskNoEarlierThan()) {
      builder.startTaskNoEarlierThan(
          TimestampInstantConverter.TO_INSTANT.convert(task.getStartTaskNoEarlierThan()));
    }
    if (task.hasDoNotCreateIterationAfter()) {
      builder.doNotCreateIterationAfter(
          TimestampInstantConverter.TO_INSTANT.convert(task.getDoNotCreateIterationAfter()));
    }
    if (task.hasStartedTime()) {
      builder.startedTime(TimestampInstantConverter.TO_INSTANT.convert(task.getStartedTime()));
    }
    if (task.hasStopTime()) {
      builder.stopTime(TimestampInstantConverter.TO_INSTANT.convert(task.getStopTime()));
    }
    if (task.hasCreatedTime()) {
      builder.createdTime(TimestampInstantConverter.TO_INSTANT.convert(task.getCreatedTime()));
    }
    if (task.hasInfo()) {
      builder.info(ProtoParser.toJsonString(task.getInfo()));
    }

    return builder.build();
  }
}
