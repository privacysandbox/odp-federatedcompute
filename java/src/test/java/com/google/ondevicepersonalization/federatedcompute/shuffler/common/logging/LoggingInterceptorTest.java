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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.ondevicepersonalization.federatedcompute.test.proto.TestRequest;
import com.google.ondevicepersonalization.federatedcompute.test.proto.TestRequestPayload;
import com.google.ondevicepersonalization.federatedcompute.test.proto.TestResponse;
import com.google.ondevicepersonalization.federatedcompute.test.proto.TestResponsePayload;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@RunWith(TestParameterInjector.class)
public final class LoggingInterceptorTest {

  private static final TestResponse DEFAULT_RESPONSE1 =
      TestResponse.newBuilder()
          .setPayload(TestResponsePayload.newBuilder().setId("1").setGroupId("2"))
          .build();
  private static final TestRequest DEFAULT_REQUEST1 =
      TestRequest.newBuilder().setPayload(TestRequestPayload.newBuilder().setName("cuiq")).build();

  @Mock HttpServletRequest mockRequest;
  @Mock HttpServletResponse mockResponse;
  @Mock ContentCachingRequestWrapper mockWrapRequest;
  @Mock ContentCachingResponseWrapper mockWrapResponse;

  @InjectMocks LoggingInterceptor loggingInterceptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testPreHandle_PassinWrap_Succeeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("get"));
    when(mockWrapRequest.getMethod()).thenReturn("GET");
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");

    // act
    boolean result = loggingInterceptor.preHandle(mockWrapRequest, mockWrapResponse, handleMethod);

    // assert
    assertThat(result).isTrue();
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Received request: GET /get-something.");
  }

  @Test
  public void testPreHandle_PassinRawrequest_Succeeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("get"));
    when(mockRequest.getMethod()).thenReturn("GET");
    when(mockRequest.getRequestURI()).thenReturn("/get-something");

    // act
    boolean result = loggingInterceptor.preHandle(mockRequest, mockResponse, handleMethod);

    // assert
    assertThat(result).isTrue();
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Received request: GET /get-something.");
  }

  @Test
  public void testAfterComplete_WrapRequest_Get_Succeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("get"));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");
    when(mockWrapResponse.getContentAsByteArray()).thenReturn(DEFAULT_RESPONSE1.toByteArray());

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(2);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage())
        .isEqualTo("response body: " + serialize(DEFAULT_RESPONSE1));
  }

  private enum CreateMethod {
    CREATE("create"),
    CREATE2("create2"),
    CREATE_NO_RETURN("createNoReturn");
    final String code;

    CreateMethod(String code) {
      this.code = code;
    }
  }

  @Test
  public void testAfterComplete_WrapRequest_Create_Succeded(@TestParameter CreateMethod method)
      throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod(
            "no_bean", FakeController.class.getMethod(method.code, TestRequest.class));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");
    when(mockWrapRequest.getContentAsByteArray()).thenReturn(DEFAULT_REQUEST1.toByteArray());
    when(mockWrapResponse.getContentAsByteArray()).thenReturn(DEFAULT_RESPONSE1.toByteArray());
    boolean returnVoid = (method == CreateMethod.CREATE_NO_RETURN);

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(returnVoid ? 2 : 3);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage())
        .isEqualTo("request body: " + serialize(DEFAULT_REQUEST1));
    if (!returnVoid) {
      assertThat(logsList.get(2).getFormattedMessage())
          .isEqualTo("response body: " + serialize(DEFAULT_RESPONSE1));
    }
  }

  @Test
  public void testAfterComplete_WrapRequest_NoPayload_Succeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("get"));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(2);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage()).isEqualTo("response body: <empty>");
  }

  @Test
  public void testAfterComplete_WrapRequest_EmptyResponsePayload_Succeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("get"));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");
    when(mockWrapResponse.getContentAsByteArray()).thenReturn(new byte[0]);

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(2);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage()).isEqualTo("response body: <empty>");
  }

  @Test
  public void testAfterComplete_WrapRequest_Create_ExceptionNoResponse_Succeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("create", TestRequest.class));
    when(mockWrapResponse.getStatus()).thenReturn(400);
    when(mockWrapRequest.getRequestURI()).thenReturn("/create-something");
    when(mockWrapRequest.getContentAsByteArray()).thenReturn(DEFAULT_REQUEST1.toByteArray());

    // act
    loggingInterceptor.afterCompletion(
        mockWrapRequest,
        mockWrapResponse,
        handleMethod,
        new IllegalArgumentException("bad request"));

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(4);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /create-something, 400 Bad Request.");
    assertThat(logsList.get(1).getFormattedMessage().contains("bad request")).isTrue();
    assertThat(logsList.get(2).getFormattedMessage())
        .isEqualTo("request body: " + serialize(DEFAULT_REQUEST1));
    assertThat(logsList.get(3).getFormattedMessage()).isEqualTo("response body: <empty>");
  }

  @Test
  public void testAfterComplete_RawRequest_Succeded() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("get"));
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockRequest.getRequestURI()).thenReturn("/get-something");

    // act
    loggingInterceptor.afterCompletion(mockRequest, mockResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(3);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage()).isEqualTo("Cannot log request payload.");
    assertThat(logsList.get(2).getFormattedMessage()).isEqualTo("Cannot log response payload.");
  }

  @Test
  public void testAfterComplete_WrapRequest_InvalidResponseType() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("getStr"));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");
    when(mockWrapResponse.getContentAsByteArray()).thenReturn("abc".getBytes());

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
  }

  @Test
  public void testAfterComplete_WrapRequest_InvalidRequestType() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod(
            "no_bean", FakeController.class.getMethod("createFromString", String.class));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");
    when(mockWrapRequest.getContentAsByteArray()).thenReturn("abc".getBytes());
    when(mockWrapResponse.getContentAsByteArray()).thenReturn(DEFAULT_RESPONSE1.toByteArray());

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(2);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage())
        .isEqualTo("response body: " + serialize(DEFAULT_RESPONSE1));
  }

  @Test
  public void testAfterComplete_UnknownHandler(@TestParameter CreateMethod method)
      throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");

    // act
    loggingInterceptor.afterCompletion(
        mockWrapRequest, mockWrapResponse, "Not A Valid Handler", null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(2);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(1).getFormattedMessage()).isEqualTo("HandlerMethod not found.");
  }

  @Test
  public void testAfterComplete_WrapRequest_InvalidRequestBytes() throws Exception {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    HandlerMethod handleMethod =
        new HandlerMethod("no_bean", FakeController.class.getMethod("create", TestRequest.class));
    when(mockWrapResponse.getStatus()).thenReturn(200);
    when(mockWrapRequest.getRequestURI()).thenReturn("/get-something");
    when(mockWrapRequest.getContentAsByteArray()).thenReturn("hello".getBytes());
    when(mockWrapResponse.getContentAsByteArray()).thenReturn(DEFAULT_RESPONSE1.toByteArray());

    // act
    loggingInterceptor.afterCompletion(mockWrapRequest, mockWrapResponse, handleMethod, null);

    // assert
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(3);
    assertThat(logsList.get(0).getFormattedMessage())
        .isEqualTo("Response for /get-something, 200 OK.");
    assertThat(logsList.get(2).getFormattedMessage())
        .isEqualTo("response body: " + serialize(DEFAULT_RESPONSE1));
  }

  private String serialize(MessageOrBuilder proto) throws Exception {
    return JsonFormat.printer().omittingInsignificantWhitespace().print(proto);
  }

  private ListAppender<ILoggingEvent> prepairListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(LoggingInterceptor.class);

    // create and start a ListAppender
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();

    // add the appender to the logger
    logger.addAppender(listAppender);
    return listAppender;
  }

  static class FakeController {
    public TestResponse create(@RequestBody TestRequest request) {
      return DEFAULT_RESPONSE1;
    }

    @ResponseProto(TestResponse.class)
    public ResponseEntity<TestResponse> create2(@RequestBody TestRequest request) {
      return ResponseEntity.status(200).body(DEFAULT_RESPONSE1);
    }

    public void createNoReturn(@RequestBody TestRequest request) {}

    public TestResponse createFromString(@RequestBody String request) {
      return DEFAULT_RESPONSE1;
    }

    public TestResponse get() {
      return DEFAULT_RESPONSE1;
    }

    public String getStr() {
      return "abc";
    }
  }
}
