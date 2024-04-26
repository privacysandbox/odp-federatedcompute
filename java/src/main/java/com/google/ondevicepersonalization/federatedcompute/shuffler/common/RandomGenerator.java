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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common;

public interface RandomGenerator {

  /**
   * Returns a pseudorandomly chosen long value between zero (inclusive) and the specified bound
   * (exclusive).
   *
   * @param bound the upper bound (exclusive) for the returned value. Must be positive.
   * @return a pseudorandomly chosen long value between zero (inclusive) and the bound (exclusive)
   * @throws IllegalArgumentException – if bound is not positive
   */
  public long nextLong(long bound);

  /**
   * Returns a pseudorandomly chosen long value between zero (inclusive) and the specified bound
   * (exclusive).
   *
   * @param bound the upper bound (exclusive) for the returned value. Must be positive.
   * @return a pseudorandomly chosen long value between zero (inclusive) and the bound (exclusive)
   * @throws IllegalArgumentException – if bound is not positive
   */
  public int nextInt(int bound);
}
