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

import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@RunWith(JUnit4.class)
public final class LoggingFilterTest {

  @Mock UniqueIdGenerator mockIdGenerator;
  @Mock FilterChain mockFilerChain;
  @Mock HttpServletRequest mockRequest;
  @Mock HttpServletResponse mockResponse;
  @Mock ContentCachingRequestWrapper mockContentCachingRequestWrapper;
  @Mock ContentCachingResponseWrapper mockContentCachingResponseWrapper;
  @InjectMocks LoggingFilter loggingFilter;

  private static final String CORELATION_ID1 = "xyz";
  private static final String ACTIVITY_ID1 = "abcdefghijklmn";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockIdGenerator.generate()).thenReturn(ACTIVITY_ID1);
  }

  @Test
  public void testdoFilterInternal_WrappeRequest() throws Exception {
    // act
    loggingFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    // assert
    verify(mockFilerChain, times(1))
        .doFilter(
            isA(ContentCachingRequestWrapper.class), isA(ContentCachingResponseWrapper.class));
  }

  @Test
  public void testdoFilterInternal_NoNeedToWrappeRequest() throws Exception {
    // act
    loggingFilter.doFilterInternal(
        mockContentCachingRequestWrapper, mockContentCachingResponseWrapper, mockFilerChain);

    // assert
    verify(mockFilerChain, times(1))
        .doFilter(mockContentCachingRequestWrapper, mockContentCachingResponseWrapper);
  }

  @Test
  public void testdoFilterInternal_NotWrappeRequest() throws Exception {
    // arrange
    when(mockRequest.getDispatcherType()).thenReturn(DispatcherType.ASYNC);

    // act
    loggingFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    // assert
    verify(mockFilerChain, times(1))
        .doFilter(isA(HttpServletRequest.class), isA(HttpServletResponse.class));
  }

  @Test
  public void testdoFilterInternal_generatedId() throws Exception {

    // act
    loggingFilter.doFilterInternal(mockRequest, mockResponse, mockFilerChain);

    // assert
    verify(mockIdGenerator, times(1)).generate();
  }
}
