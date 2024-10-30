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

import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.ODP_AUTHENTICATION_KEY;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.ODP_AUTHORIZATION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.internal.federatedcompute.v1.AuthenticationMetadata;
import com.google.internal.federatedcompute.v1.KeyAttestationAuthMetadata;
import com.google.internal.federatedcompute.v1.RejectionInfo;
import com.google.internal.federatedcompute.v1.RejectionReason;
import com.google.internal.federatedcompute.v1.RetryWindow;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentResponse;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AuthorizationTokenDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AuthorizationTokenDao.TokenStatus;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class AuthenticationFilterTest {

  @Mock private KeyAttestationManager mockKaMgr;

  @Mock private AuthorizationTokenDao mockAuthTokenDao;

  @Mock FilterChain mockFilerChain;
  @Mock HttpServletRequest mockRequest;

  @Mock HttpServletResponse mockResponse;

  @Mock ServletOutputStream mockWriter;

  @Mock UniqueIdGenerator mockIdGenerator;

  @InjectMocks AuthenticationFilter authenticationFilter;

  private static final String TOKEN = UUID.randomUUID().toString();
  private static String ATTESTATION_RECORD = "[\"MIIDcjCCAxegAwIBAgIBAT\"]";
  private static final String CHALLENGE =
      "AHXUDhoSEFikqOefmo8xE7kGp/xjVMRDYBecBiHGxCN8rTv9W0Z4L/14d0OLB"
          + "vC1VVzXBAnjgHoKLZzuJifTOaBJwGNIQ2ejnx3n6ayoRchDNCgpK29T+EAhBWzH";
  private static final String RANDOM_ID = "abcdefghijklmn";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(mockIdGenerator.generate()).thenReturn(RANDOM_ID);
    doNothing().when(mockResponse).setStatus(anyInt());
    when(mockResponse.getOutputStream()).thenReturn(mockWriter);
  }

  @Test
  public void doFilterInternal_inAllowList_authorized() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(TOKEN);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(null);
    when(mockAuthTokenDao.isTokenAuthorized(TOKEN)).thenReturn(TokenStatus.AUTHORIZED);

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain).doFilter(any(), any());
  }

  @Test
  public void doFilterInternal_attestationRecordThrows_unauthenticatedWithRetry() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(TOKEN);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(ATTESTATION_RECORD);
    when(mockAuthTokenDao.isTokenAuthorized(TOKEN)).thenReturn(TokenStatus.UNAUTHORIZED);
    doThrow(new IllegalStateException())
        .when(mockKaMgr)
        .isAttestationRecordVerified(ATTESTATION_RECORD);

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain, never()).doFilter(any(), any());
    verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
    verify(mockWriter)
        .write(
            CreateTaskAssignmentResponse.newBuilder()
                .setRejectionInfo(
                    RejectionInfo.newBuilder()
                        .setReason(RejectionReason.Enum.UNAUTHENTICATED)
                        .setRetryWindow(
                            RetryWindow.newBuilder()
                                .setDelayMin(Duration.newBuilder().setSeconds(60))
                                .setDelayMax(Duration.newBuilder().setSeconds(300))))
                .build()
                .toByteArray());
  }

  @Test
  public void doFilterInternal_recordAuthorized() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(TOKEN);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(ATTESTATION_RECORD);
    when(mockAuthTokenDao.isTokenAuthorized(TOKEN)).thenReturn(TokenStatus.UNAUTHORIZED);
    when(mockAuthTokenDao.insert(TOKEN)).thenReturn(TokenStatus.AUTHORIZED);
    when(mockKaMgr.isAttestationRecordVerified(ATTESTATION_RECORD)).thenReturn(true);

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain).doFilter(any(), any());
    verify(mockAuthTokenDao).insert(TOKEN);
  }

  @Test
  public void doFilterInternal_tokenExpired() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(TOKEN);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(null);
    when(mockAuthTokenDao.isTokenAuthorized(TOKEN)).thenReturn(TokenStatus.UNAUTHORIZED);
    when(mockKaMgr.fetchChallenge()).thenReturn(CHALLENGE.getBytes());

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain, never()).doFilter(any(), any());
    verify(mockKaMgr).fetchChallenge();
    verify(mockAuthTokenDao).isTokenAuthorized(TOKEN);
    verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockWriter)
        .write(
            CreateTaskAssignmentResponse.newBuilder()
                .setRejectionInfo(
                    RejectionInfo.newBuilder()
                        .setReason(RejectionReason.Enum.UNAUTHENTICATED)
                        .setAuthMetadata(
                            AuthenticationMetadata.newBuilder()
                                .setKeyAttestationMetadata(
                                    KeyAttestationAuthMetadata.newBuilder()
                                        .setChallenge(ByteString.copyFrom(CHALLENGE.getBytes())))))
                .build()
                .toByteArray());
  }

  @Test
  public void doFilterInternal_KAVSException_unauthorizedWithRetry() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(null);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(null);
    doThrow(new IllegalStateException()).when(mockKaMgr).fetchChallenge();

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain, never()).doFilter(any(), any());
    verify(mockKaMgr).fetchChallenge();
    verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
    verify(mockWriter)
        .write(
            CreateTaskAssignmentResponse.newBuilder()
                .setRejectionInfo(
                    RejectionInfo.newBuilder()
                        .setReason(RejectionReason.Enum.UNAUTHORIZED)
                        .setRetryWindow(
                            RetryWindow.newBuilder()
                                .setDelayMin(Duration.newBuilder().setSeconds(60))
                                .setDelayMax(Duration.newBuilder().setSeconds(300))))
                .build()
                .toByteArray());
  }

  @Test
  public void doFilterInternal_noToken() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(null);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(null);
    when(mockKaMgr.fetchChallenge()).thenReturn(CHALLENGE.getBytes());

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain, never()).doFilter(any(), any());
    verify(mockKaMgr).fetchChallenge();
    verify(mockAuthTokenDao, never()).isTokenAuthorized(anyString());
    verify(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(mockWriter)
        .write(
            CreateTaskAssignmentResponse.newBuilder()
                .setRejectionInfo(
                    RejectionInfo.newBuilder()
                        .setReason(RejectionReason.Enum.UNAUTHENTICATED)
                        .setAuthMetadata(
                            AuthenticationMetadata.newBuilder()
                                .setKeyAttestationMetadata(
                                    KeyAttestationAuthMetadata.newBuilder()
                                        .setChallenge(ByteString.copyFrom(CHALLENGE.getBytes())))))
                .build()
                .toByteArray());
  }

  @Test
  public void doFilterInternal_recordUnauthorized() throws Exception {
    when(mockRequest.getHeader(ODP_AUTHORIZATION_KEY)).thenReturn(TOKEN);
    when(mockRequest.getHeader(ODP_AUTHENTICATION_KEY)).thenReturn(ATTESTATION_RECORD);
    when(mockAuthTokenDao.isTokenAuthorized(TOKEN)).thenReturn(TokenStatus.UNAUTHORIZED);
    when(mockKaMgr.isAttestationRecordVerified(ATTESTATION_RECORD)).thenReturn(false);

    authenticationFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    verify(mockFilerChain, never()).doFilter(any(), any());
    verify(mockKaMgr).isAttestationRecordVerified(ATTESTATION_RECORD);
    verify(mockResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(mockWriter)
        .write(
            CreateTaskAssignmentResponse.newBuilder()
                .setRejectionInfo(
                    RejectionInfo.newBuilder()
                        .setReason(RejectionReason.Enum.UNAUTHORIZED)
                        .setRetryWindow(
                            RetryWindow.newBuilder()
                                .setDelayMin(Duration.newBuilder().setSeconds(60))
                                .setDelayMax(Duration.newBuilder().setSeconds(300))))
                .build()
                .toByteArray());
  }
}
