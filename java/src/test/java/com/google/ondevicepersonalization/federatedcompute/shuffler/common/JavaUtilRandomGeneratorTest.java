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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JavaUtilRandomGeneratorTest {

  private JavaUtilRandomGenerator generator = new JavaUtilRandomGenerator();

  @Test
  public void generateLong_normal_success() {
    long result = generator.nextLong(1000L);

    assertThat(result).isAtLeast(0);
    assertThat(result).isLessThan(1000L);
  }

  @Test
  public void generateLong_outOfBound_throwException() {
    assertThrows(IllegalArgumentException.class, () -> generator.nextLong(0L));
  }

  @Test
  public void generateInt_normal_success() {
    long result = generator.nextLong(1000);

    assertThat(result).isAtLeast(0);
    assertThat(result).isLessThan(1000);
  }

  @Test
  public void generateInt_outOfBound_throwException() {
    assertThrows(IllegalArgumentException.class, () -> generator.nextLong(0));
  }
}
