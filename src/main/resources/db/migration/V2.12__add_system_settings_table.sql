CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default settings
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES 
    ('org_name', 'Rookwork Inc.', 'Organization Name'),
    ('org_domain', 'rookwork.com', 'Organization Domain'),
    ('slack_notifications', 'false', 'Enable Slack Notifications'),
    ('google_integration', 'false', 'Enable Google Workspace Integration')
ON CONFLICT (setting_key) DO NOTHING;
