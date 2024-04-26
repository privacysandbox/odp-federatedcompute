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

import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * A filter to replace request and response with content caching request and response for logging.
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

  @Autowired private UniqueIdGenerator idGenerator;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (isAsyncDispatch(request)) {
      filterChain.doFilter(request, response);
    } else {
      doFilterWithWrap(wrapRequest(request), wrapResponse(response), filterChain);
    }
  }

  private void doFilterWithWrap(
      ContentCachingRequestWrapper requestWrapper,
      ContentCachingResponseWrapper responseWrapper,
      FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String correlationId = requestWrapper.getHeader(Constants.HEADER_CORRELATION_ID);
      correlationId = correlationId != null ? correlationId : "";
      MDC.put(Constants.CORRELATION_ID, correlationId);
      MDC.put(Constants.ACTIVITY_ID, idGenerator.generate());
      filterChain.doFilter(requestWrapper, responseWrapper);
    } finally {
      responseWrapper.copyBodyToResponse();
      MDC.clear();
    }
  }

  private static ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
    return (request instanceof ContentCachingRequestWrapper)
        ? (ContentCachingRequestWrapper) request
        : new ContentCachingRequestWrapper(request);
  }

  private static ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
    return (response instanceof ContentCachingResponseWrapper)
        ? (ContentCachingResponseWrapper) response
        : new ContentCachingResponseWrapper(response);
  }
}
