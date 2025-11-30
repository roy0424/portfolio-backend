-- Create auth schema
CREATE SCHEMA IF NOT EXISTS auth;

-- User Account Table (OAuth2)
CREATE TABLE auth.user_account (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    email VARCHAR(255),
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_id)
);

CREATE INDEX idx_user_account_email ON auth.user_account(email);
CREATE INDEX idx_user_account_provider ON auth.user_account(provider, provider_id);
CREATE INDEX idx_user_account_status ON auth.user_account(status);

-- User Profile Table
CREATE TABLE auth.user_profile (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL UNIQUE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    bio TEXT,
    location VARCHAR(100),
    website VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES auth.user_account(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_profile_user_id ON auth.user_profile(user_id);