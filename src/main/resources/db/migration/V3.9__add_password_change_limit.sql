ALTER TABLE users ADD COLUMN last_password_change_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN password_changes_this_month INT DEFAULT 0 NOT NULL;
