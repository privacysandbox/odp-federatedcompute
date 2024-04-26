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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.security;

import java.util.Collections;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan({"com.google.ondevicepersonalization.federatedcompute.shuffler.common.security"})
public class AuthenticationFilterRegistrationBean {

  @Bean
  FilterRegistrationBean<AuthenticationFilter> registrationBean(
      AuthenticationFilter authenticationFilter, Boolean isAuthenticationEnabled) {
    final FilterRegistrationBean<AuthenticationFilter> registrationBean =
        new FilterRegistrationBean<>();
    registrationBean.setFilter(authenticationFilter);
    registrationBean.setEnabled(isAuthenticationEnabled);
    registrationBean.setName("Task assignment authentication.");
    // Enable in create task assignment and report result. Springboot only allows wildcard in the
    // end of a path matching pattern.
    registrationBean.setUrlPatterns(
        /* urlPatterns= */ Collections.singletonList("/taskassignment/v1/population/*"));
    return registrationBean;
  }
}
