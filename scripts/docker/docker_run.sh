#!/bin/bash
# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# Exit on failure
set -e

# Print commands
set -x

docker_run_flags=(
  '--rm'
  '--interactive'
  '--env=TERM=xterm-256color'
  # Mount the repository as the workdir volume
  "--volume=$PWD:/odp-federatedcompute"
  "--volume=$HOME/.cache/bazel:/root/.cache/bazel"
  "--workdir=/odp-federatedcompute"
  # Enable Docker-in-Docker by giving the container access to the host docker
  # daemon.
  "--volume=/var/run/docker.sock:/var/run/docker.sock"
  "--volume=$HOME/.docker:/root/.docker"
  # The container uses the host docker daemon, so docker commands running in
  # the container actually access the host filesystem. Thus mount the /tmp
  # directory as a volume in the container so that it can access the outputs of
  # docker commands that write to /tmp.
  '--volume=/tmp:/tmp'
  '--volume=/dev/log:/dev/log'
)

# To run with gcp credentials mount your GCP creds and set GOOGLE_APPLICATION_CREDENTIALS env
# -v ${GOOGLE_APPLICATION_CREDENTIALS}:/tmp/keys/adc.json:ro -e GOOGLE_APPLICATION_CREDENTIALS=/tmp/keys/adc.json -v $HOME/.config/gcloud:/root/.config/gcloud

docker build --cache-from=odp-federatedcompute-build:v1 -t odp-federatedcompute-build:v1 .
# The final parameter passed will be the run with bash. All other parameters will be used as docker run parameters.
docker run "${docker_run_flags[@]}" "${@:1:$#-1}" "odp-federatedcompute-build:v1" bash -c "${@: -1}"