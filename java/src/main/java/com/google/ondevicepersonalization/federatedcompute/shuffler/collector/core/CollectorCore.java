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

package com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core;

import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorNotification;

/** Collector core interface. */
public interface CollectorCore {

  /** Process iterations in status COLLECTING. */
  public void processCollecting();

  /** Process iterations in status AGGREGATING. */
  public void processAggregating();

  /** Process timeouts for iteration in status COLLECTING. */
  public void processTimeouts();

  /** Process notifications from the Aggregator. */
  public void processAggregatorNotifications(AggregatorNotification.Attributes notification);
}
