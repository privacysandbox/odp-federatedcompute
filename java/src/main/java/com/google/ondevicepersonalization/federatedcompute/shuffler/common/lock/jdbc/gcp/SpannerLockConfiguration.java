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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.lock.jdbc.gcp;

import com.google.cloud.spanner.pgadapter.ProxyServer;
import com.google.cloud.spanner.pgadapter.metadata.OptionsMetadata;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

/** Configuration for JDBC lock */
@Configuration
public class SpannerLockConfiguration {

  @Bean
  public DefaultLockRepository DefaultLockRepository(
      String spannerInstance, String projectId, String lockDatabaseName, DataSource dataSource) {
    // Start PGAdapter process before creating lock repository
    // https://cloud.google.com/spanner/docs/pg-jdbc-connect
    // PostgreSQL default port 5432
    OptionsMetadata.Builder builder =
        OptionsMetadata.newBuilder()
            .setProject(projectId)
            .setInstance(spannerInstance)
            .setDatabase(lockDatabaseName)
            .setPort(5432);
    ProxyServer server = new ProxyServer(builder.build());
    server.startServer();
    server.awaitRunning();
    DefaultLockRepository repository = new DefaultLockRepository(dataSource);
    // 10s TTL
    repository.setTimeToLive(10 * 1000);
    // Update insertQuery for PostgreSQL
    // https://docs.spring.io/spring-integration/reference/jdbc/lock-registry.html
    repository.setInsertQuery(
        repository.getInsertQuery() + " ON CONFLICT(LOCK_KEY, REGION) DO NOTHING");
    return repository;
  }

  @Bean
  public JdbcLockRegistry jdbcLockRegistry(LockRepository lockRepository) {
    return new JdbcLockRegistry(lockRepository);
  }
}
