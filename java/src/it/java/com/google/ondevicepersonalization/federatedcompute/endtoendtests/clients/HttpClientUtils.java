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

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;

/** Utility class to provide common constance and methods for Http Client. */
public final class HttpClientUtils {

  public static final HashSet<Integer> HTTP_OK = new HashSet<>(Arrays.asList(200));
  public static final HashSet<Integer> HTTP_CREATED = new HashSet<>(Arrays.asList(201));
  public static final HashSet<Integer> HTTP_OK_AND_CREATED = new HashSet<>(Arrays.asList(200, 201));

  /** Validate {@code response} with {@code expectedStatuses}, throw exception if not match. */
  public static void validateResponseStatus(
      HttpResponse<byte[]> response, HashSet<Integer> expectedStatuses) {
    if (expectedStatuses.contains(response.statusCode())) {
      return;
    }
    throw new IllegalStateException(
        String.format(
            "Response status is not expected. Expected %s, actual %s",
            expectedStatuses, response.statusCode()));
  }

  /** Return a {@link HttpClient} with {@code Version.HTTP_2} and {@code Redirect.NORMAL}. */
  public static HttpClient buildHttpClient() {
    return HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();
  }
}
