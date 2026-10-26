DO $$
DECLARE
    status_constraint RECORD;
BEGIN
    FOR status_constraint IN
        SELECT constraint_to_replace.conname
        FROM pg_constraint constraint_to_replace
        JOIN pg_class constrained_table
            ON constrained_table.oid = constraint_to_replace.conrelid
        JOIN pg_namespace constrained_schema
            ON constrained_schema.oid = constrained_table.relnamespace
        WHERE constrained_schema.nspname = current_schema()
            AND constrained_table.relname = 'applications'
            AND constraint_to_replace.contype = 'c'
            AND EXISTS (
                SELECT 1
                FROM unnest(constraint_to_replace.conkey) AS constrained_column(attnum)
                JOIN pg_attribute constrained_attribute
                    ON constrained_attribute.attrelid = constrained_table.oid
                    AND constrained_attribute.attnum = constrained_column.attnum
                WHERE constrained_attribute.attname = 'status'
            )
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%APPLIED%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%REVIEWING%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%INTERVIEW%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%ACCEPTED%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%REJECTED%'
            AND pg_get_constraintdef(constraint_to_replace.oid) NOT LIKE '%WITHDRAWN%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.applications DROP CONSTRAINT %I',
            current_schema(),
            status_constraint.conname
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint named_constraint
        JOIN pg_class constrained_table
            ON constrained_table.oid = named_constraint.conrelid
        JOIN pg_namespace constrained_schema
            ON constrained_schema.oid = constrained_table.relnamespace
        WHERE constrained_schema.nspname = current_schema()
            AND constrained_table.relname = 'applications'
            AND named_constraint.conname = 'ck_applications_status'
    ) THEN
        ALTER TABLE applications
            ADD CONSTRAINT ck_applications_status
                CHECK (status IN (
                    'APPLIED',
                    'REVIEWING',
                    'INTERVIEW',
                    'ACCEPTED',
                    'REJECTED',
                    'WITHDRAWN'
                ));
    END IF;
END $$;

DO $$
DECLARE
    status_constraint RECORD;
BEGIN
    FOR status_constraint IN
        SELECT constraint_to_replace.conname
        FROM pg_constraint constraint_to_replace
        JOIN pg_class constrained_table
            ON constrained_table.oid = constraint_to_replace.conrelid
        JOIN pg_namespace constrained_schema
            ON constrained_schema.oid = constrained_table.relnamespace
        WHERE constrained_schema.nspname = current_schema()
            AND constrained_table.relname = 'application_status_history'
            AND constraint_to_replace.contype = 'c'
            AND EXISTS (
                SELECT 1
                FROM unnest(constraint_to_replace.conkey) AS constrained_column(attnum)
                JOIN pg_attribute constrained_attribute
                    ON constrained_attribute.attrelid = constrained_table.oid
                    AND constrained_attribute.attnum = constrained_column.attnum
                WHERE constrained_attribute.attname = 'status'
            )
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%APPLIED%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%REVIEWING%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%INTERVIEW%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%ACCEPTED%'
            AND pg_get_constraintdef(constraint_to_replace.oid) LIKE '%REJECTED%'
            AND pg_get_constraintdef(constraint_to_replace.oid) NOT LIKE '%WITHDRAWN%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.application_status_history DROP CONSTRAINT %I',
            current_schema(),
            status_constraint.conname
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint named_constraint
        JOIN pg_class constrained_table
            ON constrained_table.oid = named_constraint.conrelid
        JOIN pg_namespace constrained_schema
            ON constrained_schema.oid = constrained_table.relnamespace
        WHERE constrained_schema.nspname = current_schema()
            AND constrained_table.relname = 'application_status_history'
            AND named_constraint.conname = 'ck_application_status_history_status'
    ) THEN
        ALTER TABLE application_status_history
            ADD CONSTRAINT ck_application_status_history_status
                CHECK (status IN (
                    'APPLIED',
                    'REVIEWING',
                    'INTERVIEW',
                    'ACCEPTED',
                    'REJECTED',
                    'WITHDRAWN'
                ));
    END IF;
END $$;
