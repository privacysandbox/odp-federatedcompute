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

# Used by "Device training duration" chart

SELECT
  PopulationName,
  TaskId,
  IterationId,
  SessionId,
  MIN(UNIX_SECONDS(CreatedTime)) as CreatedTime,
  (MAX(UNIX_SECONDS(CreatedTime)) - MIN(UNIX_SECONDS(CreatedTime))) AS DurationInSecond,
FROM AssignmentStatusHistory
Where CreatedTime >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
And (Status = 0 OR Status = 1) -- 0 is ASSIGNED and 1 is LOCAL_COMPLETED
GROUP BY PopulationName, TaskId, IterationId, SessionId
Order by CreatedTime DESC;