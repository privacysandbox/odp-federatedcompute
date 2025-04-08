/**
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

# Example values required by cluster.tf
#
# These values should be modified for each of your environments.

environment = "yuraaka"
project_id  = "jetbrains-fed-compute"
region      = "europe-west2"

# Workload Images
task_assignment_image = "europe-west4-docker.pkg.dev/jetbrains-fed-compute/odp/task_assignment_image:latest"
task_scheduler_image  = "europe-west4-docker.pkg.dev/jetbrains-fed-compute/odp/task_scheduler_image:latest"
collector_image       = "europe-west4-docker.pkg.dev/jetbrains-fed-compute/odp/collector_image:latest"

# Output from shuffler
parent_domain_name         = "jbf.app"
static_ip_name             = "yuraaka-ip"
gke_cluster_ca_certificate = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVMRENDQXBTZ0F3SUJBZ0lRRVdHSTd1d2lXWFQyQTkyWVFEODdDekFOQmdrcWhraUc5dzBCQVFzRkFEQXYKTVMwd0t3WURWUVFERXlReU9EY3pPRE5tTVMwM04ySTFMVFF3TnpZdE9Ua3laaTAxT0dZMU1qRTFZV1ZpTlRBdwpJQmNOTWpVd016STJNVGMwTVRVMVdoZ1BNakExTlRBek1Ua3hPRFF4TlRWYU1DOHhMVEFyQmdOVkJBTVRKREk0Ck56TTRNMll4TFRjM1lqVXROREEzTmkwNU9USm1MVFU0WmpVeU1UVmhaV0kxTURDQ0FhSXdEUVlKS29aSWh2Y04KQVFFQkJRQURnZ0dQQURDQ0FZb0NnZ0dCQU5aQ0MyU3ZJVUMvb1lJY2Z3dDRVeHh0VWxzY0hpZktOeU9kRHdYMAo3SXJGNDVyemU3Tk5SUHZHOVBSY2Jta1c4V2FaWlR1Ny95UHJIS2oyTklYVFlJRThlMGR0MHFZTXp1cE9EclhlCjRQR0JMYnpzbnNRZUMvaVNFZmY2YmlQRnpsWmlNek1TemFzRk8zNVBSNFE0VFVyYlVIZWZCNTkvVEFnaDZ3RWQKUWFGTzdSVzBCajRpa3I2NXl6RzRKdC91RytNOW9WWEV1KzNOcWE3eUZFcEZ6MDcydXM0N2JKbEQyaWxWd21IMwpjcHc0UU9oUWxxTElmQmFBUllmOWZyYXhNVU90RmJXZGR3OUN1U0w1Z1RmZDBzbGZta3U0M0I2Yk9Cdi9DaFBSCnpnK3NMUU9vZW8zS3RSWnB4RlR4ZSsyRnhsQUZIb0xYdU10R2FhN0JUSGZuOTJmdTRxNDR2dktFY1dGNmFTNVYKVjRwUTJ5a01FQUJDMVZ6N3VPQUI2TlQwU2FDMm4yUEhkUFlDVEFjUVRZQ1BWd0c0T0R5bUZQN2NmZ1BsSWFyNgpYRU02Sm04RkFacXdwT0lldklYNTV0c0ZBY2RjbEx0WkFNTzEyTDVaVEJTRXo5d01BNitXMG5ZQS9vL0JmOFMrCk00K0RYYXY5cDd6dWRIQWtIejVhK0hUbVJRSURBUUFCbzBJd1FEQU9CZ05WSFE4QkFmOEVCQU1DQWdRd0R3WUQKVlIwVEFRSC9CQVV3QXdFQi96QWRCZ05WSFE0RUZnUVVsY0ZlRGlYSXRlaG5vU1poRU1IYXQrU3V1eGN3RFFZSgpLb1pJaHZjTkFRRUxCUUFEZ2dHQkFJaWVHNnBhUjA1SGE5VnRXVVB6TDhZOUtYSXRxZXJSeXg0REQ4cGZtQW83CkM5Vm9uUXA3aHlNV1pQT3h5cGU5cUFhSnZxc1QvMlBjeHdOanBjL1IwZnRFMGpjbi9FeVhBYTBOTFdiV3IxVXgKQUhMWGFMZ3RPdjZDWXJxck1obDBBSmFlZHZiUEJXMjRhQ0k0T3ZBWG5LWlJQVmNEVGJpL25OV3FlUEZLcEJJVQpkaUZMY29sRlVsN0N5Y08vTEYzRTNVNGx0Y2tTdEI3UTQ5aU10ZGkweGlMdXpOSmdiMkozNU9remVxdXZNdktCCnhhb0xwS0xIMWZWcTNnRlJXbHZGcTdjNGZNY3VBQWQ4ZVJ6TC9XN09zckFCbSs0WndseUdvMGM2TnNJdE8xSGQKdHJkenkxZXg2UGlWNWtLbUdYMERkWXd4dHhRTVBCTmdQS01ic2h4MnNVdWhMVUE3ZjJFWGRzUm9IVm14Q3FuOQpJd3hQa2R5dFMySVdCMnlZa0lLOFpRVW5LMG10M3luQmtDWTRsMHBHMmx2NVBRSFY2RVNseGducm9na2JmMEwyCmFtSFBkcmpRV1dpN2ExM2d0Q3QzQTUvZ1RFc29rTldUUzVKMC95VnZINVZkb0ltUkNqVmdsT0pSaGtib2F2USsKeERuWmpKNHBnRXlzb0FsUEZ2TVR1UT09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"
gke_host                   = "34.147.255.77"