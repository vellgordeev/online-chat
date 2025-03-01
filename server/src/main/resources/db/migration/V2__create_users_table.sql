CREATE TABLE IF NOT EXISTS users
(
    id             SERIAL PRIMARY KEY,
    login          VARCHAR(255) NOT NULL,
    password       TEXT         NOT NULL,
    username       VARCHAR(255) NOT NULL,
    role           role         NOT NULL DEFAULT 'USER',
    is_banned      BOOLEAN      DEFAULT FALSE,
    ban_expiration TIMESTAMP
);
