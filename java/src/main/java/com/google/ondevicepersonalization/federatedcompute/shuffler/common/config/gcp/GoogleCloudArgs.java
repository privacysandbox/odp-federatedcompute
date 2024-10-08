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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/** Google cloud configurations passed in from command line args. */
@Getter
public final class GoogleCloudArgs {

  @Parameter(names = "--project_id", description = "The project id.")
  private String projectId;

  @Parameter(names = "--environment", description = "The environment.")
  private String environment;

  @Parameter(names = "--spanner_instance", description = "The spanner instance name.")
  private String spannerInstance;

  @Parameter(names = "--task_database_name", description = "The spanner task database name.")
  private String taskDatabaseName;

  @Parameter(
      names = "--metrics_spanner_instance",
      description = "The metrics spanner instance name.")
  private String metricsSpannerInstance;

  @Parameter(names = "--metrics_database_name", description = "The spanner metrics database name.")
  private String metricsDatabaseName;

  @Parameter(
      names = "--client_gradient_bucket_template",
      description = "The client gradients bucket template.")
  private String clientGradientBucketTemplate;

  @Parameter(
      names = "--aggregated_gradient_bucket_template",
      description = "The aggregated gradients bucket template.")
  private String aggregatedGradientBucketTemplate;

  @Parameter(names = "--model_bucket_template", description = "The model and plan bucket template.")
  private String modelBucketTemplate;

  @Parameter(
      names = {"--download_plan_token_duration"},
      description = "The duration of the signed url for downloading plan in seconds.")
  private Long downloadPlanTokenDurationInSecond;

  @Parameter(
      names = "--download_checkpoint_token_duration",
      description = "The duration of the signed url for downloading checkpoint in seconds.")
  private Long downloadCheckpointTokenDurationInSecond;

  @Parameter(
      names = "--upload_gradient_token_duration",
      description = "The duration of the signed url for uploading gradients in seconds.")
  private Long uploadGradientTokenDurationInSecond;

  @Parameter(
      names = "--local_compute_timeout_minutes",
      description =
          "The duration an assignment will remain in ASSIGNED status before timing out in minutes.",
      validateWith = PositiveInteger.class)
  private int localComputeTimeoutMinutes;

  @Parameter(
      names = "--upload_timeout_minutes",
      description =
          "The duration an assignment will remain in LOCAL_COMPLETED status before timing out in"
              + " minutes.",
      validateWith = PositiveInteger.class)
  private int uploadTimeoutMinutes;

  @Parameter(
      names = "--model_updater_pubsub_topic",
      description = "The name of the model updater pubsub topic.")
  private String modelUpdaterPubsubTopic;

  @Parameter(
      names = "--model_updater_pubsub_subscription",
      description = "The name of the model updater pubsub subscription.")
  private String modelUpdaterPubsubSubscription;

  @Parameter(
      names = "--aggregator_pubsub_topic",
      description = "The name of the aggregator pubsub topic.")
  private String aggregatorPubsubTopic;

  @Parameter(
      names = "--aggregator_pubsub_subscription",
      description = "The name of the aggregator pubsub subscription.")
  private String aggregatorPubsubSubscription;

  @Parameter(
      names = "--encryption_key_service_a_base_url",
      description = "The base url of encryption key service A.")
  private String encryptionKeyServiceABaseUrl;

  @Parameter(
      names = "--encryption_key_service_b_base_url",
      description = "The base url of the encryption key service B.")
  private String encryptionKeyServiceBBaseUrl;

  @Parameter(
      names = "--encryption_key_service_a_cloudfunction_url",
      description =
          "The cloudfunction url of the encryption key service A. Used for ID token aud claim"
              + " https://cloud.google.com/docs/authentication/token-types#id-aud")
  private String encryptionKeyServiceACloudfunctionUrl;

  @Parameter(
      names = "--encryption_key_service_b_cloudfunction_url",
      description =
          "The cloudfunction url of the encryption key service B. Used for ID token aud claim"
              + " https://cloud.google.com/docs/authentication/token-types#id-aud")
  private String encryptionKeyServiceBCloudfunctionUrl;

  @Parameter(
      names = "--wip_provider_a",
      description = "The workload identity provider of the encryption key service A.")
  private String wipProviderA;

  @Parameter(
      names = "--wip_provider_b",
      description = "The workload identity provider of the encryption key service B.")
  private String wipProviderB;

  @Parameter(
      names = "--service_account_a",
      description = "The service account to impersonate of the encryption key service A.")
  private String serviceAccountA;

  @Parameter(
      names = "--service_account_b",
      description = "The service account to impersonate of the encryption key service B.")
  private String serviceAccountB;

  @Parameter(
      names = "--public_key_service_base_url",
      description = "The base url of the public key service.")
  private String publicKeyServiceBaseUrl;

  @Parameter(
      names = "--model_updater_subscriber_max_outstanding_element_count",
      description =
          "The maximum number of messages for the model updater which have not received"
              + " acknowledgments or negative acknowledgments before pausing the stream.",
      validateWith = PositiveInteger.class)
  private long modelUpdaterSubscriberMaxOutstandingElementCount;

  @Parameter(
      names = "--aggregator_subscriber_max_outstanding_element_count",
      description =
          "The maximum number of messages for the aggregator which have not received"
              + " acknowledgments or negative acknowledgments before pausing the stream.",
      validateWith = PositiveInteger.class)
  private long aggregatorSubscriberMaxOutstandingElementCount;

  @Parameter(
      names = "--collector_batch_size",
      description = "The size of aggregation batches created by the collector",
      validateWith = PositiveInteger.class)
  private int collectorBatchSize;

  @Parameter(
      names = "--compression_format",
      description = "Supported client-side file compression formats",
      variableArity = true)
  private List<CompressionFormat> compressionFormats =
      new ArrayList<>(Arrays.asList(CompressionFormat.GZIP));

  @Parameter(
      names = "--aggregator_notification_pubsub_topic",
      description = "The name of the aggregator notification pubsub topic.")
  private String aggregatorNotificationPubsubTopic;

  @Parameter(
      names = "--aggregator_notification_pubsub_subscription",
      description = "The name of the aggregator notification pubsub subscription.")
  private String aggregatorNotificationPubsubSubscription;

  @Parameter(
      names = "--model_cdn_signing_key_name",
      description = "The name of the signing key for the model CDN requests.")
  private String modelCdnSigningKeyName;

  @Parameter(
      names = "--model_cdn_signing_key_value_a",
      description = "The value of the signing key A for model CDN request.")
  private String modelCdnSigningKeyValueA;

  @Parameter(
      names = "--model_cdn_signing_key_value_b",
      description = "The value of the signing key B for model CDN request.")
  private String modelCdnSigningKeyValueB;

  @Parameter(names = "--model_cdn_endpoint", description = "The endpoint of the model CDN.")
  private String modelCdnEndpoint;

  @Parameter(
      names = "--aggregation_batch_failure_threshold",
      description =
          "The number of aggregation batches failed for an iteration before moving the iteration to"
              + " a failure state.",
      validateWith = PositiveInteger.class)
  private Long aggregationBatchFailureThreshold;
}
