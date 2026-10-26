package com.yassir.jobflow;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusHistoryMigrationIntegrationTest
        extends PostgresContainerTest {

    @Test
    void migrationBackfillsOneBaselineEventForExistingApplications()
            throws Exception {

        String schema =
                "status_history_backfill_"
                        + UUID.randomUUID().toString().replace("-", "");

        Flyway flywayToV2 = flyway(schema)
                .target(MigrationVersion.fromVersion("2"))
                .load();

        try {
            flywayToV2.migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);
                statement.executeUpdate("""
                        INSERT INTO users (
                            id,
                            created_at,
                            email,
                            first_name,
                            last_name,
                            password,
                            role,
                            updated_at,
                            enabled
                        )
                        VALUES (
                            101,
                            CURRENT_TIMESTAMP,
                            'candidate@example.com',
                            'Migration',
                            'Candidate',
                            'not-used',
                            'CANDIDATE',
                            CURRENT_TIMESTAMP,
                            TRUE
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO users (
                            id,
                            created_at,
                            email,
                            first_name,
                            last_name,
                            password,
                            role,
                            updated_at,
                            enabled
                        )
                        VALUES (
                            102,
                            CURRENT_TIMESTAMP,
                            'recruiter@example.com',
                            'Migration',
                            'Recruiter',
                            'not-used',
                            'RECRUITER',
                            CURRENT_TIMESTAMP,
                            TRUE
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO companies (
                            id,
                            created_at,
                            description,
                            location,
                            name,
                            updated_at,
                            website
                        )
                        VALUES (
                            201,
                            CURRENT_TIMESTAMP,
                            'Migration company',
                            'Remote',
                            'Migration Company',
                            CURRENT_TIMESTAMP,
                            'https://example.com'
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO jobs (
                            id,
                            active,
                            company_id,
                            created_at,
                            created_by,
                            description,
                            employment_type,
                            location,
                            title,
                            updated_at
                        )
                        VALUES (
                            301,
                            TRUE,
                            201,
                            CURRENT_TIMESTAMP,
                            102,
                            'Migration job',
                            'FULL_TIME',
                            'Remote',
                            'Migration Job',
                            CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO applications (
                            id,
                            applied_at,
                            status,
                            updated_at,
                            candidate_id,
                            job_id,
                            cv_file_name,
                            cv_file_path
                        )
                        VALUES (
                            401,
                            CURRENT_TIMESTAMP - INTERVAL '1 day',
                            'REVIEWING',
                            CURRENT_TIMESTAMP,
                            101,
                            301,
                            'candidate-cv.pdf',
                            'uploads/applications/cvs/candidate-cv.pdf'
                        )
                        """);
            }

            flyway(schema).load().migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT
                            application_id,
                            status,
                            event_type,
                            changed_by,
                            changed_by_role,
                            changed_at
                        FROM application_status_history
                        ORDER BY id
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getLong("application_id")).isEqualTo(401L);
                    assertThat(resultSet.getString("status")).isEqualTo("REVIEWING");
                    assertThat(resultSet.getString("event_type")).isEqualTo("BASELINE");
                    assertThat(resultSet.getObject("changed_by")).isNull();
                    assertThat(resultSet.getString("changed_by_role")).isNull();
                    assertThat(resultSet.getTimestamp("changed_at")).isNotNull();
                    assertThat(resultSet.next()).isFalse();
                }

                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT status FROM applications WHERE id = 401"
                )) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("status")).isEqualTo("REVIEWING");
                }
            }
        } finally {
            flyway(schema)
                    .cleanDisabled(false)
                    .load()
                    .clean();
        }
    }

    @Test
    void v4AllowsWithdrawnStatusAndPreservesExistingApplicationData()
            throws Exception {

        String schema =
                "withdrawn_status_"
                        + UUID.randomUUID().toString().replace("-", "");

        Flyway flywayToV3 = flyway(schema)
                .target(MigrationVersion.fromVersion("3"))
                .load();

        try {
            flywayToV3.migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);
                statement.executeUpdate("""
                        INSERT INTO users (
                            id,
                            created_at,
                            email,
                            first_name,
                            last_name,
                            password,
                            role,
                            updated_at,
                            enabled
                        )
                        VALUES
                            (
                                1101,
                                CURRENT_TIMESTAMP,
                                'candidate-v4@example.com',
                                'Migration',
                                'Candidate',
                                'not-used',
                                'CANDIDATE',
                                CURRENT_TIMESTAMP,
                                TRUE
                            ),
                            (
                                1102,
                                CURRENT_TIMESTAMP,
                                'recruiter-v4@example.com',
                                'Migration',
                                'Recruiter',
                                'not-used',
                                'RECRUITER',
                                CURRENT_TIMESTAMP,
                                TRUE
                            )
                        """);
                statement.executeUpdate("""
                        INSERT INTO companies (
                            id,
                            created_at,
                            description,
                            location,
                            name,
                            updated_at,
                            website
                        )
                        VALUES (
                            1201,
                            CURRENT_TIMESTAMP,
                            'Migration company',
                            'Remote',
                            'Migration Company',
                            CURRENT_TIMESTAMP,
                            'https://example.com'
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO jobs (
                            id,
                            active,
                            company_id,
                            created_at,
                            created_by,
                            description,
                            employment_type,
                            location,
                            title,
                            updated_at
                        )
                        VALUES (
                            1301,
                            TRUE,
                            1201,
                            CURRENT_TIMESTAMP,
                            1102,
                            'Migration job',
                            'FULL_TIME',
                            'Remote',
                            'Migration Job',
                            CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO applications (
                            id,
                            applied_at,
                            status,
                            updated_at,
                            candidate_id,
                            job_id,
                            cv_file_name,
                            cv_file_path
                        )
                        VALUES (
                            1401,
                            CURRENT_TIMESTAMP - INTERVAL '1 day',
                            'APPLIED',
                            CURRENT_TIMESTAMP,
                            1101,
                            1301,
                            'candidate-cv.pdf',
                            'uploads/applications/cvs/candidate-cv.pdf'
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO application_status_history (
                            application_id,
                            status,
                            changed_at,
                            event_type
                        )
                        VALUES (
                            1401,
                            'APPLIED',
                            CURRENT_TIMESTAMP - INTERVAL '1 day',
                            'BASELINE'
                        )
                        """);
            }

            flyway(schema).load().migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);
                statement.executeUpdate(
                        "UPDATE applications SET status = 'WITHDRAWN' WHERE id = 1401"
                );
                statement.executeUpdate("""
                        INSERT INTO application_status_history (
                            application_id,
                            status,
                            changed_at,
                            event_type
                        )
                        VALUES (
                            1401,
                            'WITHDRAWN',
                            CURRENT_TIMESTAMP,
                            'TRANSITION'
                        )
                        """);

                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT status, candidate_id, job_id FROM applications WHERE id = 1401"
                )) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("status")).isEqualTo("WITHDRAWN");
                    assertThat(resultSet.getLong("candidate_id")).isEqualTo(1101L);
                    assertThat(resultSet.getLong("job_id")).isEqualTo(1301L);
                }

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT status
                        FROM application_status_history
                        WHERE application_id = 1401
                        ORDER BY id
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("status")).isEqualTo("APPLIED");
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("status")).isEqualTo("WITHDRAWN");
                    assertThat(resultSet.next()).isFalse();
                }
            }
        } finally {
            flyway(schema)
                    .cleanDisabled(false)
                    .load()
                    .clean();
        }
    }

    @Test
    void v4UpdatesLegacyStatusConstraintsWithDifferentNames()
            throws Exception {

        String schema =
                "legacy_status_constraint_"
                        + UUID.randomUUID().toString().replace("-", "");

        Flyway flywayToV3 = flyway(schema)
                .target(MigrationVersion.fromVersion("3"))
                .load();

        try {
            flywayToV3.migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);
                statement.execute("""
                        ALTER TABLE applications
                            DROP CONSTRAINT ck_applications_status
                        """);
                statement.execute("""
                        ALTER TABLE applications
                            ADD CONSTRAINT applications_status_check
                                CHECK (status IN (
                                    'APPLIED',
                                    'REVIEWING',
                                    'INTERVIEW',
                                    'ACCEPTED',
                                    'REJECTED'
                                ))
                        """);
                statement.execute("""
                        ALTER TABLE application_status_history
                            DROP CONSTRAINT ck_application_status_history_status
                        """);
                statement.execute("""
                        ALTER TABLE application_status_history
                            ADD CONSTRAINT application_status_history_status_check
                                CHECK (status IN (
                                    'APPLIED',
                                    'REVIEWING',
                                    'INTERVIEW',
                                    'ACCEPTED',
                                    'REJECTED'
                                ))
                        """);
                statement.executeUpdate("""
                        INSERT INTO users (
                            id,
                            created_at,
                            email,
                            first_name,
                            last_name,
                            password,
                            role,
                            updated_at,
                            enabled
                        )
                        VALUES
                            (
                                2101,
                                CURRENT_TIMESTAMP,
                                'candidate-legacy-v4@example.com',
                                'Migration',
                                'Candidate',
                                'not-used',
                                'CANDIDATE',
                                CURRENT_TIMESTAMP,
                                TRUE
                            ),
                            (
                                2102,
                                CURRENT_TIMESTAMP,
                                'recruiter-legacy-v4@example.com',
                                'Migration',
                                'Recruiter',
                                'not-used',
                                'RECRUITER',
                                CURRENT_TIMESTAMP,
                                TRUE
                            )
                        """);
                statement.executeUpdate("""
                        INSERT INTO companies (
                            id,
                            created_at,
                            description,
                            location,
                            name,
                            updated_at,
                            website
                        )
                        VALUES (
                            2201,
                            CURRENT_TIMESTAMP,
                            'Legacy migration company',
                            'Remote',
                            'Legacy Migration Company',
                            CURRENT_TIMESTAMP,
                            'https://example.com'
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO jobs (
                            id,
                            active,
                            company_id,
                            created_at,
                            created_by,
                            description,
                            employment_type,
                            location,
                            title,
                            updated_at
                        )
                        VALUES (
                            2301,
                            TRUE,
                            2201,
                            CURRENT_TIMESTAMP,
                            2102,
                            'Legacy migration job',
                            'FULL_TIME',
                            'Remote',
                            'Legacy Migration Job',
                            CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO applications (
                            id,
                            applied_at,
                            status,
                            updated_at,
                            candidate_id,
                            job_id,
                            cv_file_name,
                            cv_file_path
                        )
                        VALUES (
                            2401,
                            CURRENT_TIMESTAMP - INTERVAL '1 day',
                            'REVIEWING',
                            CURRENT_TIMESTAMP,
                            2101,
                            2301,
                            'legacy-candidate-cv.pdf',
                            'uploads/applications/cvs/legacy-candidate-cv.pdf'
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO application_status_history (
                            application_id,
                            status,
                            changed_at,
                            event_type
                        )
                        VALUES (
                            2401,
                            'REVIEWING',
                            CURRENT_TIMESTAMP - INTERVAL '1 day',
                            'BASELINE'
                        )
                        """);
            }

            flyway(schema).load().migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);
                statement.executeUpdate(
                        "UPDATE applications SET status = 'WITHDRAWN' WHERE id = 2401"
                );
                statement.executeUpdate("""
                        INSERT INTO application_status_history (
                            application_id,
                            status,
                            changed_at,
                            event_type
                        )
                        VALUES (
                            2401,
                            'WITHDRAWN',
                            CURRENT_TIMESTAMP,
                            'TRANSITION'
                        )
                        """);

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE table_schema = current_schema()
                            AND table_name = 'applications'
                            AND constraint_type = 'CHECK'
                            AND constraint_name IN (
                                'applications_status_check',
                                'ck_applications_status'
                            )
                        ORDER BY constraint_name
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("constraint_name"))
                            .isEqualTo("ck_applications_status");
                    assertThat(resultSet.next()).isFalse();
                }

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE table_schema = current_schema()
                            AND table_name = 'application_status_history'
                            AND constraint_type = 'CHECK'
                            AND constraint_name IN (
                                'application_status_history_status_check',
                                'ck_application_status_history_status'
                            )
                        ORDER BY constraint_name
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("constraint_name"))
                            .isEqualTo("ck_application_status_history_status");
                    assertThat(resultSet.next()).isFalse();
                }

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT
                            application.status,
                            application.candidate_id,
                            application.job_id,
                            history.status AS history_status
                        FROM applications application
                        JOIN application_status_history history
                            ON history.application_id = application.id
                        WHERE application.id = 2401
                            AND history.event_type = 'TRANSITION'
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("status")).isEqualTo("WITHDRAWN");
                    assertThat(resultSet.getLong("candidate_id")).isEqualTo(2101L);
                    assertThat(resultSet.getLong("job_id")).isEqualTo(2301L);
                    assertThat(resultSet.getString("history_status"))
                            .isEqualTo("WITHDRAWN");
                    assertThat(resultSet.next()).isFalse();
                }
            }
        } finally {
            flyway(schema)
                    .cleanDisabled(false)
                    .load()
                    .clean();
        }
    }

    @Test
    void v5AddsNullableUtcInstantColumnWithoutRewritingExistingHistory()
            throws Exception {

        String schema =
                "status_history_utc_instant_"
                        + UUID.randomUUID().toString().replace("-", "");

        Flyway flywayToV4 = flyway(schema)
                .target(MigrationVersion.fromVersion("4"))
                .load();

        try {
            flywayToV4.migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);
                statement.executeUpdate("""
                        INSERT INTO users (
                            id,
                            created_at,
                            email,
                            first_name,
                            last_name,
                            password,
                            role,
                            updated_at,
                            enabled
                        )
                        VALUES (
                            3101,
                            CURRENT_TIMESTAMP,
                            'v5-candidate@example.com',
                            'V5',
                            'Candidate',
                            'not-used',
                            'CANDIDATE',
                            CURRENT_TIMESTAMP,
                            TRUE
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO users (
                            id,
                            created_at,
                            email,
                            first_name,
                            last_name,
                            password,
                            role,
                            updated_at,
                            enabled
                        )
                        VALUES (
                            3102,
                            CURRENT_TIMESTAMP,
                            'v5-recruiter@example.com',
                            'V5',
                            'Recruiter',
                            'not-used',
                            'RECRUITER',
                            CURRENT_TIMESTAMP,
                            TRUE
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO companies (
                            id,
                            created_at,
                            description,
                            location,
                            name,
                            updated_at,
                            website
                        )
                        VALUES (
                            3201,
                            CURRENT_TIMESTAMP,
                            'V5 migration company',
                            'Remote',
                            'V5 Company',
                            CURRENT_TIMESTAMP,
                            'https://example.com'
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO jobs (
                            id,
                            active,
                            company_id,
                            created_at,
                            created_by,
                            description,
                            employment_type,
                            location,
                            title,
                            updated_at
                        )
                        VALUES (
                            3301,
                            TRUE,
                            3201,
                            CURRENT_TIMESTAMP,
                            3102,
                            'V5 migration job',
                            'FULL_TIME',
                            'Remote',
                            'V5 Engineer',
                            CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO applications (
                            id,
                            applied_at,
                            status,
                            updated_at,
                            candidate_id,
                            job_id
                        )
                        VALUES (
                            3401,
                            TIMESTAMP '2026-10-10 09:00:00',
                            'APPLIED',
                            TIMESTAMP '2026-10-10 09:00:00',
                            3101,
                            3301
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO application_status_history (
                            id,
                            application_id,
                            status,
                            changed_at,
                            event_type
                        )
                        VALUES (
                            3501,
                            3401,
                            'APPLIED',
                            TIMESTAMP '2026-10-10 09:00:00',
                            'BASELINE'
                        )
                        """);
            }

            flyway(schema).load().migrate();

            try (Connection connection = openConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("SET search_path TO " + schema);

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT data_type, is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                            AND table_name = 'application_status_history'
                            AND column_name = 'changed_at_instant'
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString("data_type"))
                            .isEqualTo("timestamp with time zone");
                    assertThat(resultSet.getString("is_nullable"))
                            .isEqualTo("YES");
                    assertThat(resultSet.next()).isFalse();
                }

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT changed_at, changed_at_instant
                        FROM application_status_history
                        WHERE id = 3501
                        """)) {

                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getTimestamp("changed_at")).isNotNull();
                    assertThat(resultSet.getObject("changed_at_instant")).isNull();
                    assertThat(resultSet.next()).isFalse();
                }
            }
        } finally {
            flyway(schema)
                    .cleanDisabled(false)
                    .load()
                    .clean();
        }
    }

    private static FluentConfiguration flyway(String schema) {
        return Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:db/migration")
                .defaultSchema(schema)
                .schemas(schema);
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }
}
