CREATE TABLE users (
    id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    city VARCHAR(100),
    insurance_provider VARCHAR(120),
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    token_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE insurance_providers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_insurance_providers PRIMARY KEY (id),
    CONSTRAINT uk_insurance_name UNIQUE (name),
    CONSTRAINT uk_insurance_slug UNIQUE (slug)
);

CREATE TABLE hospitals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(220) NOT NULL,
    slug VARCHAR(240) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    rating DOUBLE NOT NULL,
    reviews INT NOT NULL,
    emergency BOOLEAN NOT NULL,
    open_24x7 BOOLEAN NOT NULL,
    type VARCHAR(140) NOT NULL,
    beds INT NOT NULL,
    accreditation VARCHAR(80) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(25),
    website VARCHAR(250),
    description VARCHAR(1500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    verified BOOLEAN NOT NULL DEFAULT TRUE,
    manager_user_id BINARY(16),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_hospitals PRIMARY KEY (id),
    CONSTRAINT uk_hospitals_name UNIQUE (name),
    CONSTRAINT uk_hospitals_slug UNIQUE (slug),
    CONSTRAINT fk_hospitals_manager FOREIGN KEY (manager_user_id) REFERENCES users (id)
);

CREATE INDEX idx_hospitals_city ON hospitals (city);
CREATE INDEX idx_hospitals_state ON hospitals (state);
CREATE INDEX idx_hospitals_rating ON hospitals (rating);
CREATE INDEX idx_hospitals_active_services ON hospitals (active, emergency, open_24x7);

CREATE TABLE hospital_specialties (
    hospital_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    specialty VARCHAR(120) NOT NULL,
    CONSTRAINT pk_hospital_specialties PRIMARY KEY (hospital_id, display_order),
    CONSTRAINT fk_specialty_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
);

CREATE TABLE hospital_insurance_providers (
    hospital_id BIGINT NOT NULL,
    insurance_provider_id BIGINT NOT NULL,
    CONSTRAINT pk_hospital_insurance PRIMARY KEY (hospital_id, insurance_provider_id),
    CONSTRAINT fk_hi_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE,
    CONSTRAINT fk_hi_insurer FOREIGN KEY (insurance_provider_id) REFERENCES insurance_providers (id)
);

CREATE INDEX idx_hi_insurer ON hospital_insurance_providers (insurance_provider_id);

CREATE TABLE refresh_tokens (
    id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    user_id BINARY(16) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    user_id BINARY(16) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);

CREATE TABLE favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BINARY(16) NOT NULL,
    hospital_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_favorites PRIMARY KEY (id),
    CONSTRAINT uk_favorite_user_hospital UNIQUE (user_id, hospital_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_user_created ON favorites (user_id, created_at);

CREATE TABLE recently_viewed (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BINARY(16) NOT NULL,
    hospital_id BIGINT NOT NULL,
    last_viewed_at DATETIME(6) NOT NULL,
    view_count INT NOT NULL,
    CONSTRAINT pk_recently_viewed PRIMARY KEY (id),
    CONSTRAINT uk_recent_user_hospital UNIQUE (user_id, hospital_id),
    CONSTRAINT fk_recent_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_recent_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
);

CREATE INDEX idx_recent_user_viewed ON recently_viewed (user_id, last_viewed_at);

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BINARY(16),
    user_message VARCHAR(1000) NOT NULL,
    assistant_message VARCHAR(4000) NOT NULL,
    language VARCHAR(5) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_user_created ON chat_messages (user_id, created_at);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BINARY(16),
    actor_email VARCHAR(190),
    action VARCHAR(80) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    resource_id VARCHAR(80),
    details VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_resource ON audit_logs (resource, resource_id);
