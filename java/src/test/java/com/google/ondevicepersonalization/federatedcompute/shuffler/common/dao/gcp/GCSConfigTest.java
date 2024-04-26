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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GCSConfigTest {

  @Test
  public void testBuilderAndEquals() {
    // arrange
    GCSConfig config1 =
        GCSConfig.builder()
            .gradientBucketTemplate("a")
            .aggregatedGradientBucketTemplate("b")
            .modelBucketTemplate("c")
            .downloadPlanTokenDurationInSecond(1)
            .downloadCheckpointTokenDurationInSecond(2)
            .uploadGradientTokenDurationInSecond(3)
            .build();
    GCSConfig config2 =
        GCSConfig.builder()
            .gradientBucketTemplate("a")
            .aggregatedGradientBucketTemplate("b")
            .modelBucketTemplate("c")
            .downloadPlanTokenDurationInSecond(1)
            .downloadCheckpointTokenDurationInSecond(2)
            .uploadGradientTokenDurationInSecond(3)
            .build();
    GCSConfig.GCSConfigBuilder builder = config2.toBuilder();

    // act
    GCSConfig config3 = builder.build();

    // assert
    assertThat(config2).isEqualTo(config1);
    assertThat(config3).isEqualTo(config1);
    assertThat(config1.hashCode()).isEqualTo(config2.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }
}
