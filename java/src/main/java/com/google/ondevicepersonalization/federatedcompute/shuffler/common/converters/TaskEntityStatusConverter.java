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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters;

import com.google.common.base.Converter;
import com.google.common.collect.EnumBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity.Status;

/** Converter between {@link Status} and {@link TaskStatus}. */
public final class TaskEntityStatusConverter extends Converter<Status, TaskStatus.Enum> {

  private static final EnumBiMap<Status, TaskStatus.Enum> STATUSES =
      EnumBiMap.create(
          ImmutableMap.<Status, TaskStatus.Enum>builder()
              .put(Status.OPEN, TaskStatus.Enum.OPEN)
              .put(Status.COMPLETED, TaskStatus.Enum.COMPLETED)
              .put(Status.CREATED, TaskStatus.Enum.CREATED)
              .put(Status.CANCELED, TaskStatus.Enum.CANCELED)
              .put(Status.FAILED, TaskStatus.Enum.FAILED)
              .buildOrThrow());

  private TaskEntityStatusConverter() {}

  public static final Converter<Status, TaskStatus.Enum> TO_TASK_STATUS =
      new TaskEntityStatusConverter();
  public static final Converter<TaskStatus.Enum, Status> TO_TASK_ENTITY_STATUS =
      TO_TASK_STATUS.reverse();

  @Override
  protected TaskStatus.Enum doForward(Status status) {
    if (STATUSES.containsKey(status)) {
      return STATUSES.get(status);
    }

    throw new IllegalArgumentException(
        String.format("Invalid TaskEntity status enum : %s", status));
  }

  @Override
  protected Status doBackward(TaskStatus.Enum taskStatus) {
    if (STATUSES.inverse().containsKey(taskStatus)) {
      return STATUSES.inverse().get(taskStatus);
    }

    throw new IllegalArgumentException(
        String.format("Invalid TaskStatus proto enum : %s", taskStatus));
  }
}
