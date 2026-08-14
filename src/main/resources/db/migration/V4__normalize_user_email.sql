-- V4__normalize_user_email.sql
-- Canonicalize account emails to lowercase and enforce it going forward.
-- The application now normalizes every email at its entry points
-- (EmailNormalizer); this aligns existing rows and adds a DB guard so any
-- future code path that skips normalization fails loudly at insert time
-- instead of silently creating a duplicate-by-case account.

-- 1. Fix existing rows. If two rows differ only by case, user_account.email's
--    UNIQUE constraint makes this UPDATE abort with a unique violation — the
--    correct loud failure. Pre-check with:
--      SELECT lower(email), count(*) FROM user_account GROUP BY 1 HAVING count(*) > 1;
UPDATE user_account SET email = lower(email) WHERE email <> lower(email);

-- 2. Guard future writes (must come after the data fix above).
ALTER TABLE user_account ADD CONSTRAINT chk_email_lowercase
    CHECK (email = lower(email));
