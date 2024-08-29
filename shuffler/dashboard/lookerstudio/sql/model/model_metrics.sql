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

# Used by "Model Metrics" chart

-- Looker cloud spanner connector has response size limit of 10 MB. See:
-- https://support.google.com/looker-studio/answer/9008245?hl=en#zippy=%2Cin-this-article.
-- The query selects metrics of active populations in the last 30 days.
-- It is an example to avoid result exceeding the limit.
-- Please update the query condition in your use case.

SELECT *
FROM ModelMetrics m1
WHERE EXISTS (
    SELECT 1
    FROM ModelMetrics m2
    WHERE m1.PopulationName = m2.PopulationName
      AND m2.CreatedTime >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
) order by CreatedTime DESC, PopulationName;