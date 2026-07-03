-- V4.0: Add length constraints to text fields for data integrity
-- WorkLog.note: TEXT -> VARCHAR(500)
-- IssueType.description: TEXT -> VARCHAR(500)
-- Truncate any existing data that exceeds the new limits (safety guard)

UPDATE work_logs SET note = LEFT(note, 500) WHERE LENGTH(note) > 500;
ALTER TABLE work_logs ALTER COLUMN note TYPE VARCHAR(500);

UPDATE issue_types SET description = LEFT(description, 500) WHERE LENGTH(description) > 500;
ALTER TABLE issue_types ALTER COLUMN description TYPE VARCHAR(500);

-- Add length constraints to user profile fields
UPDATE users SET profile_name = LEFT(profile_name, 50) WHERE LENGTH(profile_name) > 50;
ALTER TABLE users ALTER COLUMN profile_name TYPE VARCHAR(50);

UPDATE users SET job_title = LEFT(job_title, 100) WHERE LENGTH(job_title) > 100;
ALTER TABLE users ALTER COLUMN job_title TYPE VARCHAR(100);

UPDATE users SET organization = LEFT(organization, 100) WHERE LENGTH(organization) > 100;
ALTER TABLE users ALTER COLUMN organization TYPE VARCHAR(100);

UPDATE users SET location = LEFT(location, 150) WHERE LENGTH(location) > 150;
ALTER TABLE users ALTER COLUMN location TYPE VARCHAR(150);

-- Add length constraints to projects
UPDATE projects SET project_name = LEFT(project_name, 100) WHERE LENGTH(project_name) > 100;
ALTER TABLE projects ALTER COLUMN project_name TYPE VARCHAR(100);

-- Add length constraints to subtasks
UPDATE subtasks SET subtask_name = LEFT(subtask_name, 200) WHERE LENGTH(subtask_name) > 200;
ALTER TABLE subtasks ALTER COLUMN subtask_name TYPE VARCHAR(200);

-- Add length constraints to events
UPDATE events SET location = LEFT(location, 200) WHERE LENGTH(location) > 200;
ALTER TABLE events ALTER COLUMN location TYPE VARCHAR(200);

UPDATE events SET event_description = LEFT(event_description, 1000) WHERE LENGTH(event_description) > 1000;
ALTER TABLE events ALTER COLUMN event_description TYPE VARCHAR(1000);
