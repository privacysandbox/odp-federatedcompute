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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.connection.ConnectionOptions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SpannerTestHarness {

  /*
   * Interface for a Spanner emulator Test Harness
   */
  public interface Connection {

    /** Returns the database client that is used by this connection. */
    DatabaseClient getDatabaseClient();

    /** Creates and initializes the Spanner database. */
    void createDatabase() throws SQLException;

    /** Drops the created Spanner database. */
    void dropDatabase() throws SQLException;

    /** Stops the test Spanner database and does any needed cleanup. */
    void stop() throws SQLException;
  }

  private static void createInstance(Spanner service, String instanceId) throws SQLException {

    if (hasInstance(service, instanceId)) {
      return;
    }

    final String projectId = service.getOptions().getProjectId();
    final InstanceId instance = InstanceId.of(projectId, instanceId);

    // Create the instance.
    try {
      service
          .getInstanceAdminClient()
          .createInstance(
              InstanceInfo.newBuilder(instance)
                  .setDisplayName("Test Instance")
                  .setInstanceConfigId(InstanceConfigId.of(projectId, "emulator-config"))
                  .setNodeCount(1)
                  .build())
          .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new SQLException("Failed creating instance", e);
    }
  }

  private static boolean hasInstance(Spanner service, String instanceId) {
    for (Instance instance : service.getInstanceAdminClient().listInstances().iterateAll()) {
      if (instance.getId().getInstance().equals(instanceId)) return true;
    }
    return false;
  }

  private static boolean hasDatabase(Spanner service, String instanceId, String databaseId) {
    for (Database db : service.getDatabaseAdminClient().listDatabases(instanceId).iterateAll()) {
      if (db.getId().getDatabase().equals(databaseId)) return true;
    }
    return false;
  }

  private static void createDatabase(
      Spanner service, String instanceId, String databaseId, List<String> ddlStatements)
      throws SQLException {
    if (hasDatabase(service, instanceId, databaseId)) {
      return;
    }

    try {
      // Create the database
      service.getDatabaseAdminClient().createDatabase(instanceId, databaseId, ddlStatements).get();

    } catch (ExecutionException | InterruptedException e) {
      throw new SQLException("Unable to create database", e);
    }

    if (!hasDatabase(service, instanceId, databaseId)) {
      throw new SQLException("Failed creating database! Doesn't exist!");
    }
  }

  // This function is not being used currently. Can be used in cleanup.
  private static void dropDatabase(Spanner service, String instanceId, String databaseId)
      throws SQLException {
    if (hasDatabase(service, instanceId, databaseId)) {
      service.getDatabaseAdminClient().dropDatabase(instanceId, databaseId);
    }
  }

  /**
   * Returns {@link List} of DDL statement. Creating database through sdl file is not supported in
   * java client of Spanner emulator, hence parsing the SDL file is the currently adopted solution.
   *
   * @param sdlFilePath the sdl file path used to define the database.
   * @throws IOException
   */
  private static List<String> getDdlStatementsFromFile(String sdlFilePath) throws IOException {
    List<String> statements = new ArrayList<>();
    StringBuilder currentStatement = new StringBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(sdlFilePath));
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      int commentIndex = line.indexOf("--");
      if (commentIndex >= 0) {
        line = line.substring(0, commentIndex).trim(); // Remove (inline) comments
      }
      if (!line.isEmpty()) {
        currentStatement.append(line);
        if (line.endsWith(";")) {
          statements.add(currentStatement.substring(0, currentStatement.length() - 1));
          currentStatement.setLength(0); // Clear the StringBuilder
        } else {
          currentStatement.append("\n");
        }
      }
    }
    reader.close();
    return statements;
  }

  static GenericContainer<?> testContainer;

  /**
   * Creates a Spanner emulator instance and then creates a temporary database using the Spanner
   * emulator using custom inputs.
   *
   * @param projectId the GCP project Id that will be used to run the emulator
   * @param instanceId id of spanner instance to be created in the emulator
   * @param databaseId id of the database to be created in the spanner instance
   * @param sdlFilePath the sdl file path used to define the database.
   */
  public static Connection useSpannerEmulatorWithCustomInputs(
      String projectId, String instanceId, String databaseId, String sdlFilePath)
      throws SQLException, IOException {

    // Use existing emulator or launch a new one
    String spannerEmulatorHost = System.getenv("SPANNER_EMULATOR_HOST");
    if (spannerEmulatorHost == null) {

      // Create the container
      // In case we need to handle the issue mentioned in the link below,
      // https://github.com/GoogleCloudPlatform/cloud-spanner-emulator/issues/25,
      // Use "gcr.io/cloud-spanner-emulator/emulator-disable-pending-commit-ts" as image.
      final String SPANNER_EMULATOR_IMAGE = "gcr.io/cloud-spanner-emulator/emulator:latest";
      testContainer =
          new GenericContainer<>(SPANNER_EMULATOR_IMAGE)
              .withCommand()
              // file to the image and run command for creating db.
              .withExposedPorts(9010, 9020)
              .waitingFor(Wait.forListeningPort());

      // Start the container
      testContainer.start();

      // JDBC Connection
      spannerEmulatorHost =
          String.format("%s:%d", testContainer.getHost(), testContainer.getMappedPort(9010));
    }

    // Create the Spanner service
    final Spanner service =
        SpannerOptions.newBuilder()
            .setProjectId(projectId)
            .setEmulatorHost(String.format("http://%s", spannerEmulatorHost))
            .build()
            .getService();

    List<String> ddlStatements = getDdlStatementsFromFile(sdlFilePath);
    // Initialize the instance and database.
    createInstance(service, instanceId);
    createDatabase(service, instanceId, databaseId, ddlStatements);

    DatabaseId db = DatabaseId.of(projectId, instanceId, databaseId);

    return new Connection() {

      @Override
      public DatabaseClient getDatabaseClient() {
        return service.getDatabaseClient(db);
      }

      @Override
      public void createDatabase() throws SQLException {
        SpannerTestHarness.createDatabase(service, instanceId, databaseId, ddlStatements);
      }

      @Override
      public void dropDatabase() throws SQLException {
        SpannerTestHarness.dropDatabase(service, instanceId, databaseId);
      }

      @Override
      public void stop() throws SQLException {
        service.close();
        try {
          ConnectionOptions.closeSpanner();
        } catch (SpannerException e) {
          // ignore
        }
        if (testContainer != null) {
          testContainer.stop();
        }
      }
    };
  }
}
