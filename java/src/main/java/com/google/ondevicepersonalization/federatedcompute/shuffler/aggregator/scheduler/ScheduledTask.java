// Copyright 2024 Google LLC
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

package com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.scheduler;

import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyEncryptionService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to periodically fetch and cache public encryption keys.
 *
 * <p>This is also used to keep application alive
 * https://github.com/GoogleCloudPlatform/spring-cloud-gcp/blob/main/docs/src/main/asciidoc/pubsub.adoc
 * The other main entry point of this application is via events from the inbound channel adapter.
 */
@Component
public class ScheduledTask {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);
  PublicKeyEncryptionService publicKeyEncryptionService;

  public ScheduledTask(PublicKeyEncryptionService publicKeyEncryptionService) {
    this.publicKeyEncryptionService = publicKeyEncryptionService;
  }

  @Scheduled(fixedDelay = 480, timeUnit = TimeUnit.MINUTES)
  public void run() throws Exception {
    publicKeyEncryptionService.fetchNewPublicKeys();
    logger.info("Completed fetching new public keys.");
  }
}
