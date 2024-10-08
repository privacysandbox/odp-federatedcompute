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

import static com.google.common.truth.Truth.assertThat;

import com.google.fcp.aggregation.AggregationException;
import com.google.fcp.tensorflow.TensorflowException;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService.KeyFetchException;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ExceptionsTest {

  @Test
  public void testIsRetryableException_TensorflowException() {
    // act and assert
    assertThat(Exceptions.isRetryableException(new TensorflowException("tf"))).isFalse();
  }

  @Test
  public void testIsRetryableException_TensorflowExceptionInIllegalState() {
    // act and assert
    assertThat(
            Exceptions.isRetryableException(
                new IllegalStateException(new TensorflowException("tf"))))
        .isFalse();
  }

  @Test
  public void testIsRetryableException_AggregationException() {
    // act and assert
    assertThat(Exceptions.isRetryableException(new AggregationException("tf"))).isFalse();
  }

  @Test
  public void testIsRetryableException_NonRetryableException() {
    // act and assert
    assertThat(Exceptions.isRetryableException(new NonRetryableException("tf"))).isFalse();
  }

  @Test
  public void testIsRetryableException_AggregationExceptionInIllegalState() {
    // act and assert
    assertThat(
            Exceptions.isRetryableException(
                new IllegalStateException(new AggregationException("tf"))))
        .isFalse();
  }

  @Test
  public void testIsRetryableException_KeyFetchException() {
    // act and assert
    assertThat(
            Exceptions.isRetryableException(new KeyFetchException("tf", ErrorReason.KEY_NOT_FOUND)))
        .isFalse();
  }

  @Test
  public void testIsRetryableException_KeyFetchExceptionInIllegalState() {
    // act and assert
    assertThat(
            Exceptions.isRetryableException(
                new IllegalStateException(new KeyFetchException("tf", ErrorReason.KEY_NOT_FOUND))))
        .isFalse();
  }

  @Test
  public void testIsRetryableException_KeyFetchExceptionRetryable() {
    // act and assert
    assertThat(Exceptions.isRetryableException(new KeyFetchException("tf", ErrorReason.INTERNAL)))
        .isTrue();
  }

  @Test
  public void testIsRetryableException_KeyFetchExceptionInIllegalStateRetryable() {
    // act and assert
    assertThat(
            Exceptions.isRetryableException(
                new IllegalStateException(
                    new KeyFetchException("tf", ErrorReason.KEY_SERVICE_UNAVAILABLE))))
        .isTrue();
  }

  @Test
  public void testIsRetryableException_NoCause() {
    // act and assert
    assertThat(Exceptions.isRetryableException(new IllegalStateException("no reason."))).isTrue();
  }
}
