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

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.fcp.aggregation.AggregationException;
import com.google.fcp.tensorflow.TensorflowException;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService.KeyFetchException;
import com.google.scp.operator.cpio.cryptoclient.model.ErrorReason;

/** An Exception helper class. */
public class Exceptions {

  /** A function to decide if an exception is retryable. */
  public static boolean isRetryableException(Exception e) {
    if (isTensorflowException(e)
        || isAggregationException(e)
        || isNonRetryableKeyFetchException(e)
        || isNonRetryableException(e)) {
      return false;
    }
    return true;
  }

  public static boolean isTensorflowException(Exception e) {
    return (e instanceof TensorflowException)
        || (Throwables.getRootCause(e) instanceof TensorflowException);
  }

  public static boolean isAggregationException(Exception e) {
    return (e instanceof AggregationException)
        || (Throwables.getRootCause(e) instanceof AggregationException);
  }

  public static boolean isNonRetryableException(Exception e) {
    return (e instanceof NonRetryableException)
        || (Throwables.getRootCause(e) instanceof NonRetryableException);
  }

  public static boolean isNonRetryableKeyFetchException(Exception e) {
    Iterable<KeyFetchException> keyFetchExceptions =
        Iterables.filter(Throwables.getCausalChain(e), KeyFetchException.class);
    for (KeyFetchException kfe : keyFetchExceptions) {
      if (kfe.reason == ErrorReason.KEY_NOT_FOUND) {
        // https://github.com/privacysandbox/coordinator-services-and-shared-libraries/blob/main/java/com/google/scp/operator/cpio/cryptoclient/MultiPartyDecryptionKeyServiceImpl.java#L238
        return true;
      }
    }
    return false;
  }

  private Exceptions() {}
}
