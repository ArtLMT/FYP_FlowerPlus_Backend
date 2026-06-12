-- V2__add_oauth2_fields.sql
-- Add OAuth2 provider support to user_account table

ALTER TABLE user_account ADD COLUMN provider VARCHAR(50) DEFAULT 'LOCAL' NOT NULL;
ALTER TABLE user_account ADD COLUMN provider_id VARCHAR(255);
