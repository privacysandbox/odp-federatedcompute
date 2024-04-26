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

package com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients;

import static com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.HttpClientUtils.HTTP_OK;
import static com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.HttpClientUtils.buildHttpClient;
import static com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.HttpClientUtils.validateResponseStatus;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.IdTokenProvider.Option;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.GetTaskByIdResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.UUID;

/** A class that simulates possible action from device to call Task Assignment API. */
public class Partner {
  private String serverSpec;
  private Task task;

  private GoogleCredentials googleCredentials;

  /**
   * Constructs a new <code>Partner</code> object.
   *
   * @param serverSpec is FCP server uri
   */
  public Partner(String serverSpec) {
    this.serverSpec = serverSpec;
  }

  private static BlobId toBlobId(String uri) {
    // gs://<bucket_name>/<file_path_inside_bucket>
    if (!uri.startsWith("gs://")) {
      throw new IllegalArgumentException("invalid gcs uri: " + uri);
    }

    uri = uri.substring("gs://".length());
    int index = uri.indexOf('/');
    String bucketName = uri.substring(0, index);
    String objectName = uri.substring(index + 1);
    return BlobId.of(bucketName, objectName);
  }

  private static void copyObject(BlobId sourceId, BlobId targetId) {
    Storage storage = StorageOptions.newBuilder().build().getService();

    Storage.BlobTargetOption precondition;
    if (storage.get(targetId) == null) {
      // For a target object that does not yet exist, set the DoesNotExist precondition.
      // This will cause the request to fail if the object is created before the request runs.
      precondition = Storage.BlobTargetOption.doesNotExist();
    } else {
      // If the destination already exists in your bucket, instead set a generation-match
      // precondition. This will cause the request to fail if the existing object's generation
      // changes before the request runs.
      precondition = Storage.BlobTargetOption.generationMatch();
    }

    storage.copy(
        Storage.CopyRequest.newBuilder()
            .setSource(sourceId)
            .setTarget(targetId, precondition)
            .build());
  }

  private static void uploadObject(byte[] data, BlobId targetId) throws IOException {
    Storage storage = StorageOptions.newBuilder().build().getService();
    BlobInfo blobInfo = BlobInfo.newBuilder(targetId).build();
    storage.createFrom(blobInfo, new ByteArrayInputStream(data));
  }

  /** Get the {@code Task} from FCP server with {@code populationName} and {@code taskId}. */
  public Task getTask(String populationName, long taskId) throws Exception {
    URI getTaskUri =
        URI.create(
            this.serverSpec
                + "/taskmanagement/v1/population/"
                + populationName
                + "/tasks/"
                + taskId
                + ":get");

    HttpClient client = buildHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(getTaskUri)
            .GET()
            .header("odp-correlation-id", UUID.randomUUID().toString())
            .header("Authorization", "Bearer " + getIdToken(this.serverSpec))
            .build();

    HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    return parseTaskFromGetTaskByIdResponse(response);
  }

  /** Create new {@code Task} at FCP server. */
  public Task createTask(Task task) throws Exception {
    URI createTaskUri =
        URI.create(
            this.serverSpec
                + "/taskmanagement/v1/population/"
                + task.getPopulationName()
                + ":create-task");
    HttpClient client = buildHttpClient();
    byte[] body = CreateTaskRequest.newBuilder().setTask(task).build().toByteArray();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createTaskUri)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .header("Content-Type", "application/x-protobuf")
            .header("odp-correlation-id", UUID.randomUUID().toString())
            .header("Authorization", "Bearer " + getIdToken(this.serverSpec))
            .build();

    HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    this.task = parseTaskFromCreateTaskResponse(response);
    return this.task;
  }

  /** Upload file at {code sourceUri} to client plan uri. */
  public void uploadClientPlan(String sourceUri) {
    this.task.getClientOnlyPlanUrlList().forEach(t -> upload(sourceUri, t));
  }

  /** Upload byte[] to client plan uri. */
  public void uploadClientPlan(byte[] source) {
    this.task
        .getClientOnlyPlanUrlList()
        .forEach(
            t -> {
              try {
                upload(source, t);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  /** Upload file at {code sourceUri} to server plan uri. */
  public void uploadServerPlan(String sourceUri) {
    this.task.getServerPhaseUrlList().stream().forEach(t -> upload(sourceUri, t));
  }

  /** Upload byte[] to server plan uri. */
  public void uploadServerPlan(byte[] source) {
    this.task.getServerPhaseUrlList().stream()
        .forEach(
            t -> {
              try {
                upload(source, t);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  /** Upload file at {code sourceUri} to checkpoint uri. */
  public void uploadCheckpoint(String sourceUri) {
    this.task.getInitCheckpointUrlList().stream().forEach(t -> upload(sourceUri, t));
  }

  /** Upload byte[] to checkpoint uri. */
  public void uploadCheckpoint(byte[] source) {
    this.task.getInitCheckpointUrlList().stream()
        .forEach(
            t -> {
              try {
                upload(source, t);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  /** Download the file at {code uri} and return {@code byte[]}. */
  public byte[] download(String uri) {
    BlobId blobId = toBlobId(uri);
    Storage storage = StorageOptions.newBuilder().build().getService();
    Blob blob = storage.get(blobId);
    return blob.getContent();
  }

  private void upload(String source, String target) {
    System.out.println("upload source: " + source + " target: " + target);
    BlobId sourceId = toBlobId(source);
    BlobId targetId = toBlobId(target);
    copyObject(sourceId, targetId);
  }

  private void upload(byte[] source, String target) throws IOException {
    System.out.println("upload byte[] source to target: " + target);
    BlobId targetId = toBlobId(target);
    uploadObject(source, targetId);
  }

  private Task parseTaskFromGetTaskByIdResponse(HttpResponse<byte[]> response) {
    validateResponseStatus(response, HTTP_OK);

    try {
      return GetTaskByIdResponse.parseFrom(
              response.body(), ExtensionRegistryLite.getEmptyRegistry())
          .getTask();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Could not parse Task proto", e);
    }
  }

  private Task parseTaskFromCreateTaskResponse(HttpResponse<byte[]> response) {
    validateResponseStatus(response, HTTP_OK);

    try {
      return CreateTaskResponse.parseFrom(response.body(), ExtensionRegistryLite.getEmptyRegistry())
          .getTask();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Could not parse Task proto", e);
    }
  }

  private static String getIdToken(String url) throws IOException {
    // Construct the GoogleCredentials object which obtains the default configuration from your
    // working environment.
    GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();

    IdTokenCredentials idTokenCredentials =
        IdTokenCredentials.newBuilder()
            .setIdTokenProvider((IdTokenProvider) googleCredentials)
            .setTargetAudience(url)
            // Setting the ID token options.
            .setOptions(Arrays.asList(Option.FORMAT_FULL, Option.LICENSES_TRUE))
            .build();

    // Get the ID token.
    // Once you've obtained the ID token, you can use it to make an authenticated call to the
    // target audience.
    return idTokenCredentials.refreshAccessToken().getTokenValue();
  }
}
