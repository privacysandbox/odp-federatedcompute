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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SinglePartitionPartitionerTest {

  private SinglePartitionPartitioner partitioner = new SinglePartitionPartitioner();

  @Test
  public void testPartitionAlways0() {
    assertThat(partitioner.getDeviceGradientPartition("session")).isEqualTo(0);
    assertThat(partitioner.getAggregatedResultPartition("us", 2, 8)).isEqualTo(0);
    assertThat(partitioner.getPlanStoagePartition("us", 3, 9)).isEqualTo(0);
    assertThat(partitioner.getCheckpointStoagePartition("us", 4, 10)).isEqualTo(0);

    assertThat(partitioner.getDeviceGradientPartitionCount()).isEqualTo(1);
    assertThat(partitioner.getAggregatedResultPartitionCount()).isEqualTo(1);
    assertThat(partitioner.getPlanStoragePartitionCount()).isEqualTo(1);
    assertThat(partitioner.getCheckpointStoragePartitionCount()).isEqualTo(1);
  }
}
