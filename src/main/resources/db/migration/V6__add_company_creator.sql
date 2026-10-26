ALTER TABLE companies
    ADD COLUMN created_by_user_id BIGINT;

ALTER TABLE companies
    ADD CONSTRAINT fk_companies_created_by_user
    FOREIGN KEY (created_by_user_id)
    REFERENCES users (id);

CREATE INDEX idx_companies_created_by_user_id
    ON companies (created_by_user_id);
