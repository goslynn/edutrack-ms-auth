-- DDL final del schema `auth` (colapsado en beta — DB local recreable).

CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(30)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    creator_user  UUID         NOT NULL,
    updater_user  UUID         NOT NULL
);

CREATE TABLE auth.roles (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    creator_user UUID         NOT NULL,
    updater_user UUID         NOT NULL
);

CREATE TABLE auth.user_roles (
    user_id     UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id     UUID        NOT NULL REFERENCES auth.roles(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE auth.role_permissions (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID        NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    resource_uuid UUID        NOT NULL,
    flags         SMALLINT    NOT NULL CHECK (flags >= 0 AND flags <= 7),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creator_user  UUID        NOT NULL,
    updater_user  UUID        NOT NULL,
    CONSTRAINT uq_role_permissions_role_resource UNIQUE (role_id, resource_uuid)
);

CREATE TABLE auth.refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creator_user UUID        NOT NULL
);

CREATE INDEX idx_user_roles_user_id        ON auth.user_roles(user_id);
CREATE INDEX idx_role_permissions_role_id  ON auth.role_permissions(role_id);
CREATE INDEX idx_refresh_tokens_user_id    ON auth.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON auth.refresh_tokens(token_hash);
