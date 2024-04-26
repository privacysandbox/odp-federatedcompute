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

package com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater;

import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import java.time.InstantSource;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Task scheduler application. */
@Configuration
@EnableAutoConfiguration
@ComponentScan({
  "com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater",
  "com.google.ondevicepersonalization.federatedcompute.shuffler.common"
})
@IntegrationComponentScan({
  "com.google.ondevicepersonalization.federatedcompute.shuffler.collector",
  "com.google.ondevicepersonalization.federatedcompute.shuffler.common"
})
@EnableScheduling
public class ModelUpdaterApplication {

  /**
   * Entry point for the Model Updater application. This method initializes the Spring context with
   * a custom configuration file and starts the application.
   *
   * @param args Command-line arguments passed to the application
   */
  public static void main(String[] args) {
    // Change the config name from "application.properties" to "modelupdater.properties".
    System.setProperty(Constants.SPRING_CONFIG_NAME, "modelupdater");

    // Starts application.
    SpringApplication app = new SpringApplication(ModelUpdaterApplication.class);
    app.run(args);
  }

  @Bean
  InstantSource provideTimeSource() {
    return InstantSource.system();
  }

  /**
   * Creates a CommandLineRunner bean that prints a list of all beans provided by the Spring Boot
   * application context. This bean executes after the application has started.
   *
   * @param ctx The Spring application context, providing access to beans
   * @return A CommandLineRunner bean configured to print bean names
   */
  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      System.out.println("Let's inspect the beans provided by Spring Boot:");

      String[] beanNames = ctx.getBeanDefinitionNames();
      Arrays.sort(beanNames);
      for (String beanName : beanNames) {
        System.out.println(beanName);
      }
    };
  }
}
