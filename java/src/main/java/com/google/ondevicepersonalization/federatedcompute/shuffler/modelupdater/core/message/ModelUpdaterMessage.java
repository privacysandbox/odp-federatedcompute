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

package com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Message used to trigger a model updater workload. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class ModelUpdaterMessage {

  /** Bucket name of server plan to use for aggregation. */
  private String serverPlanBucket;

  /** Object key of server plan to use for aggregation. */
  private String serverPlanObject;

  /** Bucket for location of the list of intermediate gradients. */
  private String intermediateGradientBucket;

  /** Prefix for location of the list of intermediate gradients. */
  private String intermediateGradientPrefix;

  /** List of intermediate gradients to apply. */
  private List<String> intermediateGradients;

  /** Bucket of checkpoint to use for applying the aggregated gradient. */
  private String checkpointBucket;

  /** Object key of checkpoint to use for applying the aggregated gradient. */
  private String checkpointObject;

  /** Bucket to upload new checkpoint. */
  private String newCheckpointOutputBucket;

  /** Object key to upload new checkpoint. */
  private String newCheckpointOutputObject;

  /** Bucket to upload new client checkpoint. */
  private String newClientCheckpointOutputBucket;

  /** Object key to upload new client checkpoint. */
  private String newClientCheckpointOutputObject;

  /** Bucket to upload model metrics. */
  private String metricsOutputBucket;

  /** Object key to upload model metrics. */
  private String metricsOutputObject;

  /** Id of the request. */
  private String requestId;
}
