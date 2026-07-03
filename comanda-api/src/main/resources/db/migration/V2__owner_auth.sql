-- owner-auth: e-mail unico por conta (D2) + estado de rotacao do refresh token (D4).
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
ALTER TABLE users ADD COLUMN refresh_token_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN refresh_token_expires_at TIMESTAMPTZ;
