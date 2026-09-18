-- Who created an account and who changed it last (AuditableEntity).
-- NULL means no signed-in user made the change: self-registration, email
-- verification, password reset, Google login, or the admin created at startup.
-- SET NULL, like order_status_history.changed_by: deleting the actor keeps the row.
ALTER TABLE user_account
    ADD COLUMN created_by UUID REFERENCES user_account (id) ON DELETE SET NULL,
    ADD COLUMN updated_by UUID REFERENCES user_account (id) ON DELETE SET NULL;
