-- ============================================================
-- V3.0: Replace hardcoded Status enum with dynamic project_statuses
-- All operations run in a single transaction (Flyway default).
-- If any step fails, Flyway will rollback the entire migration.
-- ============================================================

-- 1. Create project_statuses table
CREATE TABLE project_statuses (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID        NOT NULL,
    status_name     VARCHAR(100) NOT NULL,
    color           VARCHAR(50),
    position        INT         NOT NULL,
    status_category VARCHAR(20) NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_project_statuses_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_position
        UNIQUE (project_id, position)
);

-- 2. Seed 3 default statuses for every existing project
--    Each project gets its own set of UUIDs, so status_id is unique per project.
INSERT INTO project_statuses (id, project_id, status_name, color, position, status_category, version)
SELECT gen_random_uuid(), id, 'To Do',       '#94a3b8', 1, 'TO_DO',       0 FROM projects
UNION ALL
SELECT gen_random_uuid(), id, 'In Progress', '#3b82f6', 2, 'IN_PROGRESS', 0 FROM projects
UNION ALL
SELECT gen_random_uuid(), id, 'Done',        '#10b981', 3, 'DONE',        0 FROM projects;

-- 3. Add nullable status_id column to issues (must be nullable first for backfill)
ALTER TABLE issues ADD COLUMN status_id UUID;

-- 4. Backfill issues.status_id from the old string value, matched by (project_id, status_category)
UPDATE issues i
SET status_id = (
    SELECT ps.id
    FROM project_statuses ps
    WHERE ps.project_id       = i.project_id
      AND ps.status_category  = 'TO_DO'
    LIMIT 1
)
WHERE i.status = 'TO_DO';

UPDATE issues i
SET status_id = (
    SELECT ps.id
    FROM project_statuses ps
    WHERE ps.project_id       = i.project_id
      AND ps.status_category  = 'IN_PROGRESS'
    LIMIT 1
)
WHERE i.status = 'IN_PROGRESS';

UPDATE issues i
SET status_id = (
    SELECT ps.id
    FROM project_statuses ps
    WHERE ps.project_id       = i.project_id
      AND ps.status_category  = 'DONE'
    LIMIT 1
)
WHERE i.status = 'DONE';

-- 5. Safety check: abort if any issue is still unmapped (would fail NOT NULL anyway)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM issues WHERE status_id IS NULL) THEN
        RAISE EXCEPTION 'Migration aborted: some issues have NULL status_id after backfill. Check for unexpected status values.';
    END IF;
END$$;

-- 6. Enforce NOT NULL and add FK
ALTER TABLE issues ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE issues
    ADD CONSTRAINT fk_issues_status
        FOREIGN KEY (status_id) REFERENCES project_statuses(id);

-- 7. Index for frequent filter/count queries (Kanban board, dashboard)
CREATE INDEX idx_issues_status_id ON issues(status_id);

-- 8. Drop old enum column
ALTER TABLE issues DROP COLUMN status;
