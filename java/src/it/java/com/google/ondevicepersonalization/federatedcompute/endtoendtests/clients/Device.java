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
import static com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.HttpClientUtils.HTTP_OK_AND_CREATED;
import static com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.HttpClientUtils.buildHttpClient;
import static com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.HttpClientUtils.validateResponseStatus;

import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.gson.Gson;
import com.google.internal.federatedcompute.v1.ClientVersion;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultRequest.Result;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.Payload;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyEncryptionService;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyFetchingService;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.scp.shared.api.util.HttpClientWrapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

/** A class that simulates action from a partner management portal to call task management API. */
public class Device {
  static {
    try {
      HybridConfig.register();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Error initializing tink.");
    }
  }

  private String serverSpec;
  private TaskAssignment taskAssignment;
  private static final String AUTHORIZED_KEY = "097952e8-9fef-414c-9621-6bc869aa48ad";

  /**
   * Constructs a new <code>Device</code> object.
   *
   * @param serverSpec is FCP server uri
   */
  public Device(String serverSpec) {
    this.serverSpec = serverSpec;
  }

  /**
   * Creates a new task assignment for a specified population.
   *
   * @param clientVersion The version code of the client making the request.
   * @param populationName The name of the population to assign the task to.
   * @return True if a task assignment is created, false otherwise.
   * @throws Exception If an error occurs during the HTTP request, response parsing, or task
   *     assignment creation.
   */
  public boolean createTaskAssignment(String clientVersion, String populationName)
      throws Exception {
    URI createTaskAssignmentUri =
        URI.create(
            this.serverSpec
                + "/taskassignment/v1/population/"
                + populationName
                + ":create-task-assignment");

    HttpClient client = buildHttpClient();
    byte[] body =
        CreateTaskAssignmentRequest.newBuilder()
            .setClientVersion(ClientVersion.newBuilder().setVersionCode(clientVersion))
            .build()
            .toByteArray();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createTaskAssignmentUri)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .header("Content-Type", "application/x-protobuf")
            .header("odp-correlation-id", UUID.randomUUID().toString())
            .header("odp-authorization-key", AUTHORIZED_KEY)
            .build();

    HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    CreateTaskAssignmentResponse createTaskAssignmentResponse =
        parseCreateTaskAssignmentResponse(response);
    switch (createTaskAssignmentResponse.getResultCase()) {
      case TASK_ASSIGNMENT:
        this.taskAssignment = createTaskAssignmentResponse.getTaskAssignment();
        System.out.println(
            "Created task assignment, task id "
                + this.taskAssignment.getTaskId()
                + ", assignment id "
                + this.taskAssignment.getAssignmentId());
        return true;
      case REJECTION_INFO:
        System.out.println(
            "Create task assignment is rejected "
                + createTaskAssignmentResponse.getRejectionInfo());
        return false;
      default:
        return false;
    }
  }

  /** Download model from the init checkpoint uri. */
  public String downloadModel() throws Exception {
    String modelUrl = taskAssignment.getInitCheckpoint().getUri();
    System.out.println("downloadModel: " + modelUrl);
    String model = downloadGcs(modelUrl);
    return model;
  }

  /** Download plan from the plan uri. */
  public String downloadPlan() throws Exception {
    String planUrl = taskAssignment.getPlan().getUri();
    String plan = downloadGcs(planUrl);
    return plan;
  }

  /** Report the failure result. */
  public void reportLocalFailed() throws Exception {
    reportLocalResult(this.taskAssignment.getSelfUri(), Result.FAILED);
  }

  /** Compresses and encrypted gradient. */
  public byte[] encryptAndCompressGradient(byte[] gradient, String url) {
    HttpClientWrapper httpClientWrapper = HttpClientWrapper.builder().build();
    PublicKeyFetchingService publicKeyFetchingService =
        new PublicKeyFetchingService(url, httpClientWrapper);
    PublicKeyEncryptionService publicKeyEncryptionService =
        new PublicKeyEncryptionService(publicKeyFetchingService);
    Payload payload =
        publicKeyEncryptionService.encryptPayload(
            CompressionUtils.compressWithGzip(gradient), new byte[0]);
    Gson gson = new Gson();
    return gson.toJson(payload).getBytes();
  }

  /** Submit the result with {@code gradients}. */
  public void submitResult(byte[] gradients) throws Exception {
    byte[] content = gradients;
    if (content == null || content.length == 0) {
      content = String.valueOf(new Random().nextInt()).getBytes();
    }

    String assignmentUri = taskAssignment.getSelfUri();
    System.out.println("submitResult: " + assignmentUri);
    upload(assignmentUri, content);
  }

  private CreateTaskAssignmentResponse parseCreateTaskAssignmentResponse(
      HttpResponse<byte[]> response) {
    validateResponseStatus(response, HTTP_OK_AND_CREATED);

    try {
      return CreateTaskAssignmentResponse.parseFrom(
          response.body(), ExtensionRegistryLite.getEmptyRegistry());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Could not parse TaskAssignment proto", e);
    }
  }

  private String downloadGcs(String url) throws Exception {
    HttpClient client = buildHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

    HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    validateResponseStatus(response, HTTP_OK);
    return new String(response.body(), StandardCharsets.UTF_8);
  }

  private void upload(String url, byte[] content) throws Exception {
    String signedUrl = getUploadUrl(url);
    HttpClient client = buildHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(signedUrl))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/octet-stream")
            .header("content-encoding", "gzip")
            .build();

    HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    validateResponseStatus(response, HTTP_OK);
  }

  private String getUploadUrl(String url) throws Exception {
    return reportLocalResult(url, Result.COMPLETED).getUploadInstruction().getUploadLocation();
  }

  private ReportResultResponse reportLocalResult(
      String assignmentUrl, ReportResultRequest.Result result) throws Exception {
    URI reportResultUri =
        URI.create(
            this.serverSpec + "/taskassignment/v1" + assignmentUrl + ":report-result?%24alt=proto");

    ReportResultRequest reportResultRequest =
        ReportResultRequest.newBuilder().setResult(result).build();

    HttpClient client = buildHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(reportResultUri)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(reportResultRequest.toByteArray()))
            .header("Content-Type", "application/x-protobuf")
            .header("odp-authorization-key", AUTHORIZED_KEY)
            .build();

    HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    return parseReportResultResponse(response);
  }

  private ReportResultResponse parseReportResultResponse(HttpResponse<byte[]> response) {
    validateResponseStatus(response, HTTP_OK);

    ReportResultResponse reportResultResponse;
    try {
      reportResultResponse =
          ReportResultResponse.parseFrom(response.body(), ExtensionRegistryLite.getEmptyRegistry());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Could not parse ReportResultResponse proto", e);
    }
    return reportResultResponse;
  }
}
