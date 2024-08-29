-- Copyright 2024 Google LLC
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

# Used by "Device upload duration" chart

SELECT
  PopulationName,
  TaskId,
  IterationId,
  SessionId,
  LocalTime AS CreatedTime,
  COALESCE(UploadTime, 0) - COALESCE(LocalTime, 0) AS DurationInSecond
FROM (
  SELECT
    PopulationName,
    TaskId,
    IterationId,
    SessionId,
    MIN(CASE WHEN Status = 1 THEN UNIX_SECONDS(CreatedTime) END) AS LocalTime,
    MIN(CASE WHEN Status = 2 THEN UNIX_SECONDS(CreatedTime) END) AS UploadTime
  FROM AssignmentStatusHistory
  WHERE (Status = 1 OR Status = 2) -- 1 is LOCAL_COMPLETED and 2 is UPLOAD_COMPLETED
  GROUP BY PopulationName, TaskId, IterationId, SessionId
  HAVING COUNT(DISTINCT Status) = 2
) AS ranked_events
ORDER BY LocalTime DESC;