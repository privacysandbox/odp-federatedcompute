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

package com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Message used to trigger an aggregation workload. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class AggregatorMessage {
  /**
   * Boolean specifying whether this aggregation is an accumulation of intermediate updates (as
   * opposed to client updates).
   */
  private boolean accumulateIntermediateUpdates;

  /** Bucket name of server plan to use for aggregation. */
  private String serverPlanBucket;

  /** Object key of server plan to use for aggregation. */
  private String serverPlanObject;

  /** Bucket for location of the list of gradients. */
  private String gradientBucket;

  /** Prefix for location of the list of gradients. */
  private String gradientPrefix;

  /** List of gradients to aggregate. */
  private List<String> gradients;

  /** Bucket to upload aggregated gradient. */
  private String aggregatedGradientOutputBucket;

  /** Object key to upload aggregated gradient. */
  private String aggregatedGradientOutputObject;

  /** Id of the request. */
  private String requestId;
}
