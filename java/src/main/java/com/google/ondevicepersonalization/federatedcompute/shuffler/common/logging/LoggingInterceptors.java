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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.logging;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class for LoggingInterceptor. */
public final class LoggingInterceptors {
  private LoggingInterceptors() {}

  private static final String URL_REGEX =
      "\\b((?:https?|ftp|file):\\/\\/[-a-zA-Z0-9+\\\\&@#\\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\\/%=~_|])";
  private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

  public static String redactUrl(String content) {
    Matcher matcher = URL_PATTERN.matcher(content);
    ArrayList<String> urls = new ArrayList<>();
    while (matcher.find()) {
      urls.add(matcher.group());
    }
    String result = content;
    for (String url : urls) {
      result = result.replace(url, getUrlWithoutParameters(url));
    }

    return result;
  }

  private static String getUrlWithoutParameters(String url) {
    int index = url.indexOf('?');
    return index >= 0 ? url.substring(0, index) : url;
  }
}
