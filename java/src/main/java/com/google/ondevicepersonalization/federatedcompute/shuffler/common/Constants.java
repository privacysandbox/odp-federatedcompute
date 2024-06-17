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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common;

import java.time.Duration;
import java.util.Set;

/** Constants used by the shuffler. */
public final class Constants {
  private Constants() {}

  public static final long MAX_POPULATION_NAME_LENGTH = 64;
  public static final long MAX_TASK_ID_LENGTH = 128;
  public static final long MAX_ASSIGNMENT_ID_LENGTH = 64;
  public static final long MAX_FILE_NAME_LENGTH = 256;

  public static final String SPRING_CONFIG_NAME = "spring.config.name";
  public static final String ACTIVITY_ID = "activity.id";
  public static final String CORRELATION_ID = "correlation.id";
  public static final String ITERATION_ID = "iteration.id";
  public static final String STATUS_ID = "status.id";
  public static final String REQUEST_ID = "request.id";

  public static final String HEADER_CORRELATION_ID = "odp-correlation-id";
  public static final String HEADER_IDEMPOTENCY_KEY = "odp-idempotency-key";

  // Active assignment status code should be less than 100.
  // The range must be [0, 99]
  public static final long MAX_ACTIVE_ASSIGNMENT_STATUS_CODE = 99;

  public static final long FIRST_ASSIGNMENT_STATUS_ID = 1;

  public static final long FIRST_ITERATION_STATUS_ID = 1;
  public static final long FIRST_AGGREGATION_BATCH_STATUS_ID = 1;

  // Name of uploaded gradient files
  public static final String GRADIENT_FILE = "gradient";

  // Name of environment variable to set for input params.
  public static final String FCP_OPTS = "FCP_OPTS";

  // Name of environment variable to set for security params.
  public static final String SECURITY_OPTS = "SECURITY_OPTS";

  /** Initial interval before the first retry. */
  public static final Duration COORDINATOR_HTTPCLIENT_RETRY_INITIAL_INTERVAL =
      Duration.ofSeconds(5);

  /** Multiplier to increase interval after the first retry. */
  public static final double COORDINATOR_HTTPCLIENT_RETRY_MULTIPLIER = 3.0;

  /** Maximum number of attempts to make. */
  public static final int COORDINATOR_HTTPCLIENT_MAX_ATTEMPTS = 6;

  // Identifier of MinimumSeparationPolicy. It is used as id at EligibilityPolicyEvalSpec
  public static final String MIN_SEPARATION_POLICY_ID = "min_sep_policy";

  public static final String KEY_ATTESTATION_VERIFICAITON_OK =
      "KEY_ATTESTATION_RECORD_VERIFICATION_RESULT_OK";

  public static final Set<String> FEDERATED_COMPUTE_PACKAGE_ALLOWED =
      Set.of("com.google.android.federatedcompute", "com.android.federatedcompute");

  public static final String ODP_AUTHORIZATION_KEY = "odp-authorization-key";

  public static final String ODP_AUTHENTICATION_KEY = "odp-authentication-key";

  public static final String COMPRESSION_FORMAT_GZIP = "gzip";
}
