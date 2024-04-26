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

import static com.google.common.truth.Truth.assertThat;

import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TaskEntityConverterTest {
  private static final String POPULATION_NAME = "population_name";
  private static final String CORRELATION_ID = "correlation_id";
  private static final String MIN_CLIENT_VERSION = "min_client_version";
  private static final String MAX_CLIENT_VERSION = "max_client_version";
  private static final long TASK_ID = 1L;
  private static final long TOTAL_ITERATION = 1L;
  private static final long SIZE = 1L;
  private static final long MAX_PARALLEL = 1L;
  private static final TaskEntity.Status TASK_ENTITY_STATUS = TaskEntity.Status.OPEN;
  private static final TaskStatus.Enum TASK_STATUS = TaskStatus.Enum.OPEN;
  private static final Instant INSTANT_TIME = Instant.EPOCH;
  private static final Timestamp TIMESTAMP = Timestamp.newBuilder().setSeconds(0).build();
  private static final TaskInfo TASK_INFO = TaskInfo.newBuilder().setTrafficWeight(1).build();
  private static final String INFO_STRING = "{\n  \"trafficWeight\": \"1\"\n}";

  @Test
  public void testTaskEntityToTaskConversion_withAllValues_convertsAllValues() {
    TaskEntity taskEntity =
        TaskEntity.builder()
            .populationName(POPULATION_NAME)
            .taskId(TASK_ID)
            .totalIteration(TOTAL_ITERATION)
            .minAggregationSize(SIZE)
            .maxAggregationSize(SIZE)
            .status(TASK_ENTITY_STATUS)
            .startTaskNoEarlierThan(INSTANT_TIME)
            .doNotCreateIterationAfter(INSTANT_TIME)
            .maxParallel(MAX_PARALLEL)
            .correlationId(CORRELATION_ID)
            .minClientVersion(MIN_CLIENT_VERSION)
            .maxClientVersion(MAX_CLIENT_VERSION)
            .startedTime(INSTANT_TIME)
            .stopTime(INSTANT_TIME)
            .createdTime(INSTANT_TIME)
            .build();

    Task task = TaskEntityConverter.TO_TASK.convert(taskEntity);

    assertThat(task)
        .isEqualTo(
            Task.newBuilder()
                .setPopulationName(POPULATION_NAME)
                .setTaskId(TASK_ID)
                .setTotalIteration(TOTAL_ITERATION)
                .setMinAggregationSize(SIZE)
                .setMaxAggregationSize(SIZE)
                .setStatus(TASK_STATUS)
                .setStartedTime(TIMESTAMP)
                .setCreatedTime(TIMESTAMP)
                .setStopTime(TIMESTAMP)
                .setStartTaskNoEarlierThan(TIMESTAMP)
                .setDoNotCreateIterationAfter(TIMESTAMP)
                .setMaxParallel(MAX_PARALLEL)
                .setCorrelationId(CORRELATION_ID)
                .setMinClientVersion(MIN_CLIENT_VERSION)
                .setMaxClientVersion(MAX_CLIENT_VERSION)
                .build());
  }

  @Test
  public void testTaskEntityToTaskConversion_withRequiredValues_convertsAvailableValues() {
    TaskEntity taskEntity =
        TaskEntity.builder()
            .populationName(POPULATION_NAME)
            .taskId(TASK_ID)
            .totalIteration(TOTAL_ITERATION)
            .minAggregationSize(SIZE)
            .maxAggregationSize(SIZE)
            .status(TASK_ENTITY_STATUS)
            .maxParallel(MAX_PARALLEL)
            .correlationId(CORRELATION_ID)
            .minClientVersion(MIN_CLIENT_VERSION)
            .maxClientVersion(MAX_CLIENT_VERSION)
            .build();

    Task task = TaskEntityConverter.TO_TASK.convert(taskEntity);

    assertThat(task)
        .isEqualTo(
            Task.newBuilder()
                .setPopulationName(POPULATION_NAME)
                .setTaskId(TASK_ID)
                .setTotalIteration(TOTAL_ITERATION)
                .setMinAggregationSize(SIZE)
                .setMaxAggregationSize(SIZE)
                .setStatus(TASK_STATUS)
                .setMaxParallel(MAX_PARALLEL)
                .setCorrelationId(CORRELATION_ID)
                .setMinClientVersion(MIN_CLIENT_VERSION)
                .setMaxClientVersion(MAX_CLIENT_VERSION)
                .build());
  }

  @Test
  public void testTaskEntityToTaskConversion_withInfo_convertsInfo() {
    TaskEntity taskEntity =
        TaskEntity.builder()
            .populationName(POPULATION_NAME)
            .taskId(TASK_ID)
            .totalIteration(TOTAL_ITERATION)
            .minAggregationSize(SIZE)
            .maxAggregationSize(SIZE)
            .status(TASK_ENTITY_STATUS)
            .maxParallel(MAX_PARALLEL)
            .correlationId(CORRELATION_ID)
            .minClientVersion(MIN_CLIENT_VERSION)
            .maxClientVersion(MAX_CLIENT_VERSION)
            .info(INFO_STRING)
            .build();

    Task task = TaskEntityConverter.TO_TASK.convert(taskEntity);

    assertThat(task)
        .isEqualTo(
            Task.newBuilder()
                .setPopulationName(POPULATION_NAME)
                .setTaskId(TASK_ID)
                .setTotalIteration(TOTAL_ITERATION)
                .setMinAggregationSize(SIZE)
                .setMaxAggregationSize(SIZE)
                .setStatus(TASK_STATUS)
                .setMaxParallel(MAX_PARALLEL)
                .setCorrelationId(CORRELATION_ID)
                .setMinClientVersion(MIN_CLIENT_VERSION)
                .setMaxClientVersion(MAX_CLIENT_VERSION)
                .setInfo(TASK_INFO)
                .build());
  }

  @Test
  public void testTaskToTaskEntityConversion_withAllValues_convertsAllValues() {
    Task task =
        Task.newBuilder()
            .setPopulationName(POPULATION_NAME)
            .setTaskId(TASK_ID)
            .setTotalIteration(TOTAL_ITERATION)
            .setMinAggregationSize(SIZE)
            .setMaxAggregationSize(SIZE)
            .setStatus(TASK_STATUS)
            .setStartedTime(TIMESTAMP)
            .setCreatedTime(TIMESTAMP)
            .setStopTime(TIMESTAMP)
            .setStartTaskNoEarlierThan(TIMESTAMP)
            .setDoNotCreateIterationAfter(TIMESTAMP)
            .setMaxParallel(MAX_PARALLEL)
            .setCorrelationId(CORRELATION_ID)
            .setMinClientVersion(MIN_CLIENT_VERSION)
            .setMaxClientVersion(MAX_CLIENT_VERSION)
            .setInfo(TASK_INFO)
            .build();

    TaskEntity taskEntity = TaskEntityConverter.TO_TASK_ENTITY.convert(task);

    assertThat(taskEntity)
        .isEqualTo(
            TaskEntity.builder()
                .populationName(POPULATION_NAME)
                .taskId(TASK_ID)
                .totalIteration(TOTAL_ITERATION)
                .minAggregationSize(SIZE)
                .maxAggregationSize(SIZE)
                .status(TASK_ENTITY_STATUS)
                .startTaskNoEarlierThan(INSTANT_TIME)
                .doNotCreateIterationAfter(INSTANT_TIME)
                .maxParallel(MAX_PARALLEL)
                .correlationId(CORRELATION_ID)
                .minClientVersion(MIN_CLIENT_VERSION)
                .maxClientVersion(MAX_CLIENT_VERSION)
                .startedTime(INSTANT_TIME)
                .stopTime(INSTANT_TIME)
                .createdTime(INSTANT_TIME)
                .info(INFO_STRING)
                .build());
  }

  @Test
  public void testTaskToTaskEntityConversion_withRequiredValues_convertsAvailableValues() {
    Task task =
        Task.newBuilder()
            .setPopulationName(POPULATION_NAME)
            .setTaskId(TASK_ID)
            .setTotalIteration(TOTAL_ITERATION)
            .setMinAggregationSize(SIZE)
            .setMaxAggregationSize(SIZE)
            .setStatus(TASK_STATUS)
            .setMaxParallel(MAX_PARALLEL)
            .setCorrelationId(CORRELATION_ID)
            .setMinClientVersion(MIN_CLIENT_VERSION)
            .setMaxClientVersion(MAX_CLIENT_VERSION)
            .setInfo(TASK_INFO)
            .build();

    TaskEntity taskEntity = TaskEntityConverter.TO_TASK_ENTITY.convert(task);

    assertThat(taskEntity)
        .isEqualTo(
            TaskEntity.builder()
                .populationName(POPULATION_NAME)
                .taskId(TASK_ID)
                .totalIteration(TOTAL_ITERATION)
                .minAggregationSize(SIZE)
                .maxAggregationSize(SIZE)
                .status(TASK_ENTITY_STATUS)
                .maxParallel(MAX_PARALLEL)
                .correlationId(CORRELATION_ID)
                .minClientVersion(MIN_CLIENT_VERSION)
                .maxClientVersion(MAX_CLIENT_VERSION)
                .info(INFO_STRING)
                .build());
  }
}
