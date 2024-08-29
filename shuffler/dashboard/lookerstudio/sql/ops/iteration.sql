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

SELECT
  I.PopulationName AS PopulationName,
  I.TaskId AS TaskId,
  I.IterationId AS IterationId,
  I.AttemptId AS AttemptId,
  CASE
    WHEN I.Status = 0 THEN 'COLLECTING'
    WHEN I.Status = 1 THEN 'AGGREGATING'
    WHEN I.Status = 2 THEN 'COMPLETED'
    WHEN I.Status = 3 THEN 'STOPPED'
    WHEN I.Status = 4 THEN 'APPLYING'
    WHEN I.Status = 5 THEN 'POST_PROCESSED'
    WHEN I.Status = 101 THEN 'CANCELED'
    WHEN I.Status = 102 THEN 'AGGREGATING_FAILED'
    WHEN I.Status = 103 THEN 'APPLYING_FAILED'
    ELSE 'Unknown'
    END AS Status,
  I.ReportGoal AS ReportGoal,
  ISH.CreatedTime AS LastUpdatedTime
FROM Iteration AS I
FULL JOIN IterationStatusHistory AS ISH
  ON
    I.PopulationName = ISH.PopulationName
    AND I.TaskId = ISH.TaskId
    AND I.IterationId = ISH.IterationId
    AND I.AttemptId = ISH.AttemptId
    AND I.Status = ISH.Status
Where I.PopulationName is not NULL
And ISH.CreatedTime >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
ORDER BY CreatedTime DESC;