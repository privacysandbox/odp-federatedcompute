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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaoConfigurations {

  @Bean
  DatabaseClient provideDatabaseClient(
      String projectId, String spannerInstance, String taskDatabaseName) {
    SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
    Spanner spanner = options.getService();
    DatabaseId db = DatabaseId.of(options.getProjectId(), spannerInstance, taskDatabaseName);
    DatabaseClient dbClient = spanner.getDatabaseClient(db);
    return dbClient;
  }

  @Bean
  GCSConfig gcsConfig(
      String clientGradientBucketTemplate,
      String aggregatedGradientBucketTemplate,
      String modelBucketTemplate,
      long downloadPlanTokenDurationInSecond,
      long downloadCheckpointTokenDurationInSecond,
      long uploadGradientTokenDurationInSecond) {
    return GCSConfig.builder()
        .gradientBucketTemplate(clientGradientBucketTemplate)
        .aggregatedGradientBucketTemplate(aggregatedGradientBucketTemplate)
        .modelBucketTemplate(modelBucketTemplate)
        .downloadPlanTokenDurationInSecond(downloadPlanTokenDurationInSecond)
        .downloadCheckpointTokenDurationInSecond(downloadCheckpointTokenDurationInSecond)
        .uploadGradientTokenDurationInSecond(uploadGradientTokenDurationInSecond)
        .build();
  }

  @Bean
  Storage provideStorage() {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    return storage;
  }
}
