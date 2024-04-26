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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    logger.info("Received request: {} {}.", request.getMethod(), request.getRequestURI());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    var status = response.getStatus();
    logger.info(
        "Response for {}, {} {}.",
        request.getRequestURI(),
        status,
        HttpStatus.valueOf(status).getReasonPhrase());

    if (ex != null) {
      logger.error(ex.getMessage());
    }

    if (!(handler instanceof HandlerMethod)) {
      logger.info("HandlerMethod not found.");
      return;
    }

    HandlerMethod handlerMethod = (HandlerMethod) handler;

    logRequestPayload(handlerMethod, request);
    logResponsePayload(handlerMethod, response);
  }

  private void logPayload(Class protoClass, byte[] bytesContent, String name) {
    if (bytesContent == null || bytesContent.length == 0) {
      logger.info(name + ": <empty>");
      return;
    }

    Method parseFromMethod;
    try {
      parseFromMethod = protoClass.getMethod("parseFrom", byte[].class);
    } catch (NoSuchMethodException ex) {
      return;
    }

    MessageOrBuilder protoObject;
    try {
      protoObject = (MessageOrBuilder) parseFromMethod.invoke(null, bytesContent);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      logger.error(ex.getMessage());
      return;
    }

    try {
      logger.info(
          name
              + ": "
              + LoggingInterceptors.redactUrl(
                  JsonFormat.printer().omittingInsignificantWhitespace().print(protoObject)));
    } catch (InvalidProtocolBufferException ex) {
      logger.error(name + ": <failed to parse>");
      return;
    }
  }

  private Optional<Class> getRequestPayloadType(HandlerMethod handlerMethod) {
    for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
      if (parameter.getParameterAnnotation(RequestBody.class) != null) {
        return Optional.of(parameter.getParameterType());
      }
    }

    return Optional.empty();
  }

  private Optional<Class> getResponsePlayloadType(HandlerMethod handlerMethod) {
    MethodParameter returnType = handlerMethod.getReturnType();
    if (returnType == null) {
      return Optional.empty();
    }

    if (handlerMethod.hasMethodAnnotation(ResponseProto.class)) {
      ResponseProto annotation = handlerMethod.getMethodAnnotation(ResponseProto.class);
      return Optional.of(annotation.value());
    }

    return Optional.of(returnType.getParameterType());
  }

  private void logRequestPayload(HandlerMethod handlerMethod, HttpServletRequest request) {
    if (!(request instanceof ContentCachingRequestWrapper)) {
      logger.info("Cannot log request payload.");
      return;
    }

    ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
    byte[] requestBytes = cachingRequest.getContentAsByteArray();
    getRequestPayloadType(handlerMethod)
        .ifPresent(proto -> logPayload(proto, requestBytes, "request body"));
  }

  private void logResponsePayload(HandlerMethod handlerMethod, HttpServletResponse response) {
    if (!(response instanceof ContentCachingResponseWrapper)) {
      logger.info("Cannot log response payload.");
      return;
    }

    ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;
    byte[] responseBytes = cachingResponse.getContentAsByteArray();
    getResponsePlayloadType(handlerMethod)
        .ifPresent(proto -> logPayload(proto, responseBytes, "response body"));
  }
}
