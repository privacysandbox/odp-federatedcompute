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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.cloud.spanner.DatabaseClient;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AuthorizationTokenDao.TokenStatus;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;

@RunWith(JUnit4.class)
public class AuthorizationTokenSpannerDaoTest {

  private static final String PROJECT_ID = "spanner-emulator";

  private static final String SDL_FILE_PATH = "shuffler/spanner/task_database.sdl";

  private static final String INSTANCE = "fcp-task-unittest";

  private static final String DB_NAME = "authorization-token-dao";

  private static SpannerTestHarness.Connection spannerEmulatorConnection;

  private DatabaseClient dbClient;

  private static InstantSource instantSource;

  @InjectMocks AuthorizationTokenSpannerDao dao;

  @BeforeClass
  public static void setup() throws SQLException, IOException {
    spannerEmulatorConnection =
        SpannerTestHarness.useSpannerEmulatorWithCustomInputs(
            PROJECT_ID, INSTANCE, DB_NAME, SDL_FILE_PATH);
  }

  @AfterClass
  public static void cleanup() throws SQLException {
    spannerEmulatorConnection.stop();
  }

  @Before
  public void initializeDatabase() throws SQLException {
    spannerEmulatorConnection.createDatabase();
    this.instantSource = spy(InstantSource.system());
    this.dbClient = spannerEmulatorConnection.getDatabaseClient();
    this.dao = new AuthorizationTokenSpannerDao(dbClient, instantSource);
  }

  @After
  public void teardown() throws SQLException {
    spannerEmulatorConnection.dropDatabase();
  }

  @Test
  public void testIsTokenVerified_noToken() {
    TokenStatus exists = dao.isTokenAuthorized(UUID.randomUUID().toString());

    assertThat(exists).isEqualTo(TokenStatus.UNAUTHORIZED);
  }

  @Test
  public void testinsertAndVerified() {
    String token = UUID.randomUUID().toString();
    assertThat(dao.insert(token)).isEqualTo(TokenStatus.AUTHORIZED);

    TokenStatus exists = dao.isTokenAuthorized(token);
    assertThat(exists).isEqualTo(TokenStatus.AUTHORIZED);
  }

  @Test
  public void testInsertDuplicate() {
    String token = UUID.randomUUID().toString();
    assertThat(dao.insert(token)).isEqualTo(TokenStatus.AUTHORIZED);
    assertThat(dao.insert(token)).isEqualTo(TokenStatus.AUTHORIZED);
  }

  @Test
  public void testInsertAndExpired() {
    String token = UUID.randomUUID().toString();
    TokenStatus inserted = dao.insert(token);
    assertThat(inserted).isEqualTo(TokenStatus.AUTHORIZED);

    Instant nowInstant = instantSource.instant();
    long eightDaysInSeconds = 8 * 24 * 60 * 60L;
    Instant eightDaysLater = nowInstant.plusSeconds(eightDaysInSeconds);
    doReturn(eightDaysLater).when(instantSource).instant();
    assertThat(dao.isTokenAuthorized(token)).isEqualTo(TokenStatus.UNAUTHORIZED);
  }
}
