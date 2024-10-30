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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** A Task Data Access class. */
public interface TaskDao {

  /** Get task by id. */
  public Optional<TaskEntity> getTaskById(String populationName, long taskId);

  /** Create a new task. */
  public TaskEntity createTask(TaskEntity entity);

  /** Get active tasks. */
  public List<TaskEntity> getActiveTasks();

  /** Get tasks with CREATED status. */
  public List<TaskEntity> getCreatedTasks();

  /** Get iteration by id. */
  public Optional<IterationEntity> getIterationById(IterationId iterationId);

  /** Get the last iteration of the given task. */
  public Optional<IterationEntity> getLastIterationOfTask(String populationName, long taskId);

  /** Get the open iterations and tasks of given population name and client version. */
  public List<IterationEntity> getOpenIterations(String populationName, String clientVersion);

  /** Create new iteration. */
  public IterationEntity createIteration(IterationEntity iteration);

  /**
   * Update iteration status.
   *
   * @return If updates successfully.
   */
  public boolean updateIterationStatus(IterationEntity from, IterationEntity to);

  /**
   * Update task status.
   *
   * @return If updates successfully.
   */
  public boolean updateTaskStatus(TaskId taskId, TaskEntity.Status from, TaskEntity.Status to);

  /** Get the iterations of a status. */
  public List<IterationEntity> getIterationsOfStatus(IterationEntity.Status status);

  /**
   * Retrieves a list of iteration IDs of {@code populationName} and {@code taskId} per every {@code
   * iterationInterval} iterations that occurred within past {@code withInPastHours} hours.
   *
   * <p>An example, there are 10 records with IterationId from 1 to 10, assume they are all meet
   * target condition, with iterationInterval=3 the selected IterationId list is 1, 4, 7, 10.
   *
   * @param populationName The name of the population to query.
   * @param taskId The ID of the task to which the iterations belong.
   * @param iterationInterval The interval at which to select iterations. Only iterations whose IDs
   *     are divisible by 'iterationInterval' will be included.
   * @param withInPastHours The number of hours in the past to consider. Only iterations created
   *     within this timeframe will be included.
   * @return A list of Long values representing the IDs of the selected iterations.
   */
  public List<Long> getIterationIdsPerEveryKIterationsSelector(
      String populationName, long taskId, long iterationInterval, long withInPastHours);

  /**
   * Retrieves a list of iteration IDs of {@code populationName} and {@code taskId} that occurred
   * within past {@code withInHours}, per below algorithms: Split the time by the {@code
   * intervalInHours} duration from the beginning of the UNIX epoch. Find the earliest iteration
   * created iterations in each bucket.
   *
   * <p>An example, assume there is a new record every 2000 second, and all of them are with past 24
   * hours. Below is a view of the data: | IterationId | CreatedTime(UNIX_SECONDS) |
   * DurationNumber(floor(CreatedTime/3600)) | |1|1693520000|470422| |2|1693522000|470422|
   * |3|1693524000|470423| |4|1693526000|470423| |5|1693528000|470424| |6|1693530000|470425|
   * |7|1693532000|470425| |8|1693534000|470426|
   *
   * <p>The query with duration_in_seconds=3600, withinHours=24, will select the Min(IterationId) in
   * each DurationNumber, which are rows with IterationId 1,3,5,6,8
   *
   * @param populationName The name of the population to query.
   * @param taskId The ID of the task to which the iterations belong.
   * @param intervalInHours The duration that split iterations by created time.
   * @param withInHours The number of hours in the past to consider. Only iterations created within
   *     this timeframe will be included.
   * @return A list of Long values representing the IDs of the selected iterations.
   */
  public List<Long> getIterationIdsPerEveryKHoursSelector(
      String populationName, long taskId, long intervalInHours, long withInHours);

  /** Get the {@code CreatedTime} of a given {@code IterationEntity} */
  public Optional<Instant> getIterationCreatedTime(IterationEntity iterationEntity);
}
