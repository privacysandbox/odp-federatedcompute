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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import static java.lang.String.format;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AuthorizationTokenDao;
import java.time.Instant;
import java.time.InstantSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Spanner implementation of AuthorizationTokenDao. */
@Component
public class AuthorizationTokenSpannerDao implements AuthorizationTokenDao {

  private static final Logger logger = LoggerFactory.getLogger(AuthorizationTokenSpannerDao.class);

  private final DatabaseClient dbClient;

  private final InstantSource instantSource;

  private static final long TOKEN_TTL_IN_SECONDS = 7 * 24 * 60 * 60L;

  public AuthorizationTokenSpannerDao(
      @Qualifier("taskDatabaseClient") DatabaseClient dbClient, InstantSource instantSource) {
    this.dbClient = dbClient;
    this.instantSource = instantSource;
  }

  public TokenStatus isTokenAuthorized(String token) {
    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());

    Statement statement =
        Statement.newBuilder(
                "SELECT Token, ExpiredAt FROM AllowedAuthorizationToken \n"
                    + "WHERE Token = @token AND ExpiredAt > @now")
            .bind("token")
            .to(token)
            .bind("now")
            .to(now)
            .build();
    try (ResultSet rs = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      if (rs.next()) {
        return TokenStatus.AUTHORIZED;
      } else {
        return TokenStatus.UNAUTHORIZED;
      }
    } catch (SpannerException e) {
      logger.atWarn().setCause(e).log(format("Failed to verify token %s", token));
      return TokenStatus.UNAUTHORIZED;
    }
  }

  public TokenStatus insert(String token) {
    Instant nowInstant = instantSource.instant();
    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(nowInstant);
    Timestamp expirationTime =
        TimestampInstantConverter.TO_TIMESTAMP.convert(
            instantSource.instant().plusSeconds(TOKEN_TTL_IN_SECONDS));
    Statement statement =
        Statement.newBuilder(
                "INSERT INTO AllowedAuthorizationToken (Token, CreatedAt, ExpiredAt) \n"
                    + "VALUES(@token, @now, @expirationTime)")
            .bind("token")
            .to(token)
            .bind("now")
            .to(now)
            .bind("expirationTime")
            .to(expirationTime)
            .build();

    try {
      return dbClient
          .readWriteTransaction()
          .run(
              transaction -> {
                long rowsInserted = transaction.executeUpdate(statement);
                if (rowsInserted == 1L) {
                  return TokenStatus.AUTHORIZED;
                } else {
                  return TokenStatus.UNKNOWN;
                }
              });
    } catch (SpannerException e) {
      if (e.getErrorCode() == ErrorCode.ALREADY_EXISTS) {
        return TokenStatus.AUTHORIZED;
      }
      logger.atWarn().setCause(e).log(format("Failed to insert token %s", token));
    }
    return TokenStatus.UNKNOWN;
  }
}
