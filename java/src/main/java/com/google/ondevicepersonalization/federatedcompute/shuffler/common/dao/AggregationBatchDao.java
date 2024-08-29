/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import java.util.List;
import java.util.Optional;

/** Aggregation batch DAO. */
public interface AggregationBatchDao {

  /**
   * Update aggregation batch status.
   *
   * @return If updates successfully.
   */
  public boolean updateAggregationBatchStatus(
      AggregationBatchEntity from, AggregationBatchEntity to);

  /** Query aggregation batches of status and optional createdByPartition for an iteration. */
  List<String> queryAggregationBatchIdsOfStatus(
      IterationEntity iteration,
      long AggregationLevel,
      AggregationBatchEntity.Status status,
      Optional<String> createdByPartition);

  /** Returns the sum of all batchSizes of batches in provided status for an iteration */
  public long querySumOfAggregationBatchesOfStatus(
      IterationEntity iteration, long AggregationLevel, List<AggregationBatchEntity.Status> status);

  /** Get aggregation batch by id. */
  public Optional<AggregationBatchEntity> getAggregationBatchById(
      AggregationBatchId aggregationBatchId);

  /**
   * Creates a new batch and updates the given list of batches status to UPLOADED_COMPLETED and sets
   * aggregatedBy to the new batch,
   *
   * @return If updates successfully
   */
  public boolean createAndAssignBatches(
      List<AggregationBatchId> batchesToUpdate, AggregationBatchEntity newBatch);
}
