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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.security;

import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.ACTIVITY_ID;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.CORRELATION_ID;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.ODP_AUTHENTICATION_KEY;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.ODP_AUTHORIZATION_KEY;

import com.google.internal.federatedcompute.v1.AuthenticationMetadata;
import com.google.internal.federatedcompute.v1.KeyAttestationAuthMetadata;
import com.google.internal.federatedcompute.v1.RejectionInfo;
import com.google.internal.federatedcompute.v1.RejectionReason;
import com.google.internal.federatedcompute.v1.RetryWindow;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentResponse;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AuthorizationTokenDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AuthorizationTokenDao.TokenStatus;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ComponentScan({
  "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao",
  "com.google.ondevicepersonalization.federatedcompute.shuffler.common.security"
})
public class AuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

  @Autowired private UniqueIdGenerator idGenerator;

  @Autowired private KeyAttestationManager keyAttestationManager;

  @Autowired private AuthorizationTokenDao authorizationTokenDao;

  public static final String CONTENT_TYPE_HDR = "Content-Type";
  public static final String PROTOBUF_CONTENT_TYPE = "application/x-protobuf";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = request.getHeader(Constants.HEADER_CORRELATION_ID);
    correlationId = correlationId != null ? correlationId : "";
    MDC.put(CORRELATION_ID, correlationId);
    MDC.put(ACTIVITY_ID, idGenerator.generate());

    String authorizationKey = request.getHeader(ODP_AUTHORIZATION_KEY);
    String authenticationKey = request.getHeader(ODP_AUTHENTICATION_KEY);
    if (isAuthorizationKeyInAllowList(authorizationKey)) {
      logger.debug("Passed authorization key allow list");
      filterChain.doFilter(request, response);
      return;
    }

    if (authenticationKey == null) {
      response.setHeader(CONTENT_TYPE_HDR, PROTOBUF_CONTENT_TYPE);
      // No authorization token and authentication record, ask the device to solve a challenge.
      try {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        byte[] challenge = keyAttestationManager.fetchChallenge();
        response.getOutputStream().write(getUnauthenticatedResponse(challenge).toByteArray());
      } catch (IllegalStateException e) {
        logger.error("Failed to fetch challenge, ask the client to retry later.", e);
        response.setStatus(HttpServletResponse.SC_OK);
        response
            .getOutputStream()
            .write(getRetryResponse(RejectionReason.Enum.UNAUTHORIZED, 60, 300).toByteArray());
      } finally {
        MDC.clear();
        return;
      }
    }

    try {
      // check the authentication record is a valid or not
      if (keyAttestationManager.isAttestationRecordVerified(authenticationKey)) {
        logger.info("Verifying attestation record with length " + authorizationKey.length());
        authorizationTokenDao.insert(authorizationKey);
        filterChain.doFilter(request, response);
      } else {
        // The authentication record shows the device is an unauthorized one.
        response.setHeader(CONTENT_TYPE_HDR, PROTOBUF_CONTENT_TYPE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getOutputStream().write(getUnauthorizedResponse().toByteArray());
      }
    } catch (IllegalStateException e) {
      logger.error("Failed to verify attestation record, ask the client to retry later.", e);
      response.setHeader(CONTENT_TYPE_HDR, PROTOBUF_CONTENT_TYPE);
      response.setStatus(HttpServletResponse.SC_OK);
      response
          .getOutputStream()
          .write(getRetryResponse(RejectionReason.Enum.UNAUTHENTICATED, 60, 300).toByteArray());
    } finally {
      MDC.clear();
    }
  }

  private boolean isAuthorizationKeyInAllowList(String authorizationKey) {
    return authorizationKey != null
        && authorizationTokenDao.isTokenAuthorized(authorizationKey) == TokenStatus.AUTHORIZED;
  }

  private CreateTaskAssignmentResponse getUnauthorizedResponse() {
    return CreateTaskAssignmentResponse.newBuilder()
        .setRejectionInfo(
            RejectionInfo.newBuilder()
                .setReason(RejectionReason.Enum.UNAUTHORIZED)
                .setRetryWindow(
                    RetryWindow.newBuilder()
                        .setDelayMin(Duration.newBuilder().setSeconds(60))
                        .setDelayMax(Duration.newBuilder().setSeconds(300))))
        .build();
  }

  private CreateTaskAssignmentResponse getUnauthenticatedResponse(byte[] challenge) {
    return CreateTaskAssignmentResponse.newBuilder()
        .setRejectionInfo(
            RejectionInfo.newBuilder()
                .setReason(RejectionReason.Enum.UNAUTHENTICATED)
                .setAuthMetadata(
                    AuthenticationMetadata.newBuilder()
                        .setKeyAttestationMetadata(
                            KeyAttestationAuthMetadata.newBuilder()
                                .setChallenge(ByteString.copyFrom(challenge)))))
        .build();
  }

  private CreateTaskAssignmentResponse getRetryResponse(
      RejectionReason.Enum rejectionReason, int minRetrySecond, int maxRetrySecond) {
    return CreateTaskAssignmentResponse.newBuilder()
        .setRejectionInfo(
            RejectionInfo.newBuilder()
                .setReason(rejectionReason)
                .setRetryWindow(
                    RetryWindow.newBuilder()
                        .setDelayMin(Duration.newBuilder().setSeconds(minRetrySecond))
                        .setDelayMax(Duration.newBuilder().setSeconds(maxRetrySecond))))
        .build();
  }
}
