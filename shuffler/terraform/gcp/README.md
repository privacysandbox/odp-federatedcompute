## Overview

Welcome to the README for setting up GCP federated compute shuffler!

Please follow the instructions below to set up federated compute shuffler in GCP.

## Pre-requisites

1. Install Terraform
2. Setup GCP credentials by setting `GOOGLE_APPLICATION_CREDENTIALS`
   - https://cloud.google.com/docs/authentication/gcloud
3. Enable required GCP APIs
```
gcloud services enable \
    artifactregistry.googleapis.com \
    container.googleapis.com \
    iam.googleapis.com \
    confidentialcomputing.googleapis.com \
    spanner.googleapis.com \
    domains.googleapis.com \
    dns.googleapis.com \
    run.googleapis.com \
    pubsub.googleapis.com \
    certificatemanager.googleapis.com \
    compute.googleapis.com \
    logging.googleapis.com \
    secretmanager.googleapis.com
 ```
4. Setup a managed public zone using Cloud DNS for your domain. The name of the zone should match the domain name with `-`'s replacing any `.` in the name. The terraform will use the DNS Zone for the domain for the external load balancer's IP.
   - https://cloud.google.com/dns/docs/set-up-dns-records-domain-name
   - Example:
     - For domain `fcp.odp-androidtest.dev` the managed zone created is `fcp-odp-androidtest-dev`.
5. (Recommended) Create compute service account to use for confidential space.
   - This account will need to be allow-listed by the coordinator operators after creation.

## Instructions

### Deploy a Shuffler Infrastructure

1. Create a new directory in environments directory and copy the contents of
   `environments/dev` to the new environment directory. The `environments/dev` contains the minimum setup needed for deploying an instance of the federated compute shuffler.
```bash
cp -avR environments/dev environments/<new_env>
```
2. file `main.tf`:
    - terraform configurations
        - uncomment the `backend "gcs"` block and populate:
           - bucket: gcs bucket to store the terraform state
           - prefix: prefix of the path to store the terraform state
3. Edit the following required fields in `dev.auto.tfvars`:
    - `environment`: The environment name
    - `project_id`: The GCP project id
    - `region`: The GCP region where all services will be created
    - `parent_domain_name`: The domain name to use for external APIs. A DNS Zone should exist for this in Cloud Domains.
    - `spanner_instance_config`: Instance configuration for the created Spanner tables. https://cloud.google.com/spanner/docs/instance-configurations
4. Fields under `Workload Images` should be updated to point to the corresponding workload container images.
5. Fields under `Coordinator Configuration` should be updated to the values provided by the Coordinator operators.
6. Additional configuration variables are available and defined under `shuffler_variables.tf`
7. a. (Recommended) Create compute service account to use for confidential space. 
     - This account will need to be allow-listed by the coordinator operators after creation. 

   b. If not provided through `dev.auto.tfvars`, compute service accounts will be created for the aggregator and model updater workloads.
     - If not provided, the services accounts generated will be formatted as:
       - ModelUpdater: `<environment>-mu@<project-id>.iam.gserviceaccount.com`
       - Aggregator: `<environment>-ag@<project-id>.iam.gserviceaccount.com`
       - These will need to be allow-listed by the coordinator operators after creation. After allow-listed, any running aggregator and model updater instance groups should replace all instances.
8. Apply the terraform:
```bash
terraform init
terraform plan
terraform apply -auto-approve
```
9. Retrieve and save output for the following for the following deployments
   - Cluster:
     - `static_ip_name`
     - `gke_cluster_ca_certificate`
     - `gke_host`
   - Database:
     - `spanner_database_name`
     - `spanner_instance_name`
```bash
terraform output -json
```

### Deploy Cluster Infrastructure

1. Go to the `cluster` directory
2. Edit the following required fields in the terraform configuration
      file `main.tf`:
   - terraform configurations
      - uncomment the `backend "gcs"` block and populate:
         - bucket: gcs bucket to store the terraform state
         - prefix: prefix of the path to store the terraform state
3. Edit the following required fields in `dev.auto.tfvars`:
   - `environment`: The environment name
   - `project_id`: The GCP project id
   - `region`: The GCP region where all services will be created
   - `task_assignment_image`: the task assignment container image
   - `task_scheduler_image`: the task scheduler container image
   - `collector_image`: the collector container image
   - `parent_domain_name`: custom domain name to use for external APIs. Should match what was used for shuffler
   - `static_ip_name`: output from shuffler
   - `gke_cluster_ca_certificate`: output from shuffler
   - `gke_host`: output from shuffler
4. Apply the terraform:

```bash
terraform init
terraform plan
terraform apply -auto-approve
```
5. Note: After deployment, the server will not be reachable until the provisioned SSL certificate is active.
```
gcloud compute ssl-certificates describe <environment>-cert
```

### Create database tables
1. From the repository root run the below command replacing `spanner_instance_name` and `spanner_database_name` with the output from shuffler:
```bash
gcloud spanner databases ddl update <spanner_database_name> --instance=<spanner_instance_name> --ddl-file=shuffler/spanner/task_database.sdl
```