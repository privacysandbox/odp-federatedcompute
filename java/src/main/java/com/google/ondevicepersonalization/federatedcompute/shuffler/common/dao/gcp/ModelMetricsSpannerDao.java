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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Value;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ModelMetricsSpannerDao implements ModelMetricsDao {
  private static final Logger logger = LoggerFactory.getLogger(ModelMetricsSpannerDao.class);

  private DatabaseClient dbClient;

  public ModelMetricsSpannerDao(@Qualifier("metricsDatabaseClient") DatabaseClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public boolean upsertModelMetrics(Collection<ModelMetricsEntity> modelMetrics) {
    try {
      List<Mutation> mutations = new ArrayList<>();
      return dbClient
          .readWriteTransaction()
          .run(
              transaction -> {
                for (ModelMetricsEntity modelMetric : modelMetrics) {
                  mutations.add(
                      Mutation.newInsertOrUpdateBuilder("ModelMetrics")
                          .set("PopulationName")
                          .to(modelMetric.getPopulationName())
                          .set("TaskId")
                          .to(modelMetric.getTaskId())
                          .set("IterationId")
                          .to(modelMetric.getIterationId())
                          .set("MetricName")
                          .to(modelMetric.getMetricName())
                          .set("MetricValue")
                          .to(modelMetric.getMetricValue())
                          .set("CreatedTime")
                          .to(Value.COMMIT_TIMESTAMP)
                          .build());
                }
                transaction.buffer(mutations);
                return true;
              })
          .booleanValue();
    } catch (Exception e) {
      logger.atWarn().setCause(e).log("Failed to upsert model metrics");
      return false;
    }
  }
}
