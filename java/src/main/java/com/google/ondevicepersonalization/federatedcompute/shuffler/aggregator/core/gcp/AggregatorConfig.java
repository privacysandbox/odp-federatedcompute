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

package com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.gcp;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.fcp.tensorflow.AppFiles;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for aggregator. */
@Configuration
public class AggregatorConfig {

  @Bean
  public Storage provideStorage() {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    return storage;
  }

  @Bean
  @Qualifier("pubSubSubscription")
  public String pubSubSubscription(String aggregatorPubsubSubscription) {
    // Set the pubSubSubscription bean to point to aggregatorPubsubSubscription
    return aggregatorPubsubSubscription;
  }

  @Bean
  public AppFiles provideTensorflowAppFiles() {
    // Use "/dev/shm" for now as it is the only available ram-mapped volume in Confidential Space.
    // This is needed to improve the performance of tensorflow IO.
    return new AppFiles("/dev/shm");
  }
}
