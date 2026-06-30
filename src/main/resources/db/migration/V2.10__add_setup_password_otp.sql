ALTER TABLE users ADD COLUMN IF NOT EXISTS setup_password_otp VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS setup_password_otp_expires_at TIMESTAMP;
