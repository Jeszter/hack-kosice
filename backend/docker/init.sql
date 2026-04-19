CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       pin_hash VARCHAR(255),
                       display_name VARCHAR(100),
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                device_id VARCHAR(128),
                                device_name VARCHAR(128),
                                expires_at TIMESTAMPTZ NOT NULL,
                                revoked_at TIMESTAMPTZ,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_hash ON refresh_tokens(token_hash);

CREATE TABLE accounts (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          name VARCHAR(100) NOT NULL,
                          balance_cents BIGINT NOT NULL DEFAULT 0,
                          currency CHAR(3) NOT NULL DEFAULT 'EUR',
                          owner_user_id UUID NOT NULL REFERENCES users(id),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE account_members (
                                 account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 role VARCHAR(20) NOT NULL DEFAULT 'member',
                                 contributed_cents BIGINT NOT NULL DEFAULT 0,
                                 joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 PRIMARY KEY (account_id, user_id)
);

CREATE TABLE bank_connections (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  bank_code VARCHAR(50) NOT NULL,
                                  external_account_id VARCHAR(128) NOT NULL,
                                  consent_id VARCHAR(128) NOT NULL,
                                  consent_token TEXT NOT NULL,
                                  consent_expires_at TIMESTAMPTZ NOT NULL,
                                  active BOOLEAN NOT NULL DEFAULT TRUE,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_user ON bank_connections(user_id);

CREATE TABLE virtual_cards (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               last4 CHAR(4) NOT NULL,
                               provider VARCHAR(50) NOT NULL,
                               external_card_id VARCHAR(128),
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               frozen BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                              initiator_user_id UUID NOT NULL REFERENCES users(id),
                              merchant VARCHAR(255),
                              category VARCHAR(50),
                              total_cents BIGINT NOT NULL,
                              currency CHAR(3) NOT NULL DEFAULT 'EUR',
                              split_mode VARCHAR(20) NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'pending',
                              executed_at TIMESTAMPTZ,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tx_account ON transactions(account_id, created_at DESC);

CREATE TABLE transaction_splits (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
                                    user_id UUID NOT NULL REFERENCES users(id),
                                    bank_connection_id UUID REFERENCES bank_connections(id),
                                    amount_cents BIGINT NOT NULL,
                                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                                    external_payment_id VARCHAR(128),
                                    failure_reason TEXT,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_split_tx ON transaction_splits(transaction_id);

CREATE TABLE audit_log (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id UUID REFERENCES users(id),
                           event_type VARCHAR(50) NOT NULL,
                           ip_address INET,
                           user_agent TEXT,
                           metadata JSONB,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_user_time ON audit_log(user_id, created_at DESC);