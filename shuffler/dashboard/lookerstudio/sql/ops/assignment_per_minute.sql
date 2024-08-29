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

# Used by "Assignments per minute [Status]" chart

SELECT
  ANY_VALUE(A.PopulationName) AS PopulationName,
  ANY_VALUE(A.TaskId) AS TaskId,
  CASE
    WHEN A.Status = 0 THEN 'ASSIGNED'
    WHEN A.Status = 1 THEN 'LOCAL_COMPLETED'
    WHEN A.Status = 2 THEN 'UPLOAD_COMPLETED'
    WHEN A.Status = 101 THEN 'CANCELED'
    WHEN A.Status = 102 THEN 'LOCAL_FAILED'
    WHEN A.Status = 103 THEN 'LOCAL_NOT_ELIGIBLE'
    WHEN A.Status = 151 THEN 'LOCAL_TIMEOUT'
    WHEN A.Status = 152 THEN 'UPLOAD_TIMEOUT'
    ELSE 'Unknown'
END
  AS Status,
  ANY_VALUE(TIMESTAMP_TRUNC(A.CreatedTime, MINUTE)) AS CreatedTimeMinute,
  COUNT(DISTINCT(A.SessionId)) AS CountPerMinute
FROM (
    # History table tracks changes of Status or BatchId. For some status,
    # 1 SessionId may have more than 1 records in history table,
    # use this query to count 1 for 1 SessionId
  SELECT
    ANY_VALUE(A.PopulationName) AS PopulationName,
    ANY_VALUE(A.TaskId) AS TaskId,
    ANY_VALUE(A.IterationId) AS IterationId,
    ANY_VALUE(A.SessionId) AS SessionId,
    ANY_VALUE(A.Status) AS Status,
    MIN(A.CreatedTime) AS CreatedTime,
  FROM
    AssignmentStatusHistory AS A
  WHERE
    # set to different value for different charts, e.g. 2 for UPLOAD_COMPLETED.
    Status = 2
    AND A.CreatedTime >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
  GROUP BY
    A.SessionId ) AS A
WHERE
  # set to different value for different charts, e.g. 2 for UPLOAD_COMPLETED.
  A.Status = 2
GROUP BY
  A.PopulationName,
  A.TaskId,
  A.Status,
  TIMESTAMP_TRUNC(A.CreatedTime, MINUTE)
ORDER BY
  CreatedTimeMinute,
  Status;