ALTER TABLE issues ADD COLUMN start_date TIMESTAMPTZ;
UPDATE issues SET start_date = created_at WHERE start_date IS NULL;

CREATE TABLE issue_dependencies (
    issue_id UUID NOT NULL,
    depends_on_id UUID NOT NULL,
    PRIMARY KEY (issue_id, depends_on_id),
    CONSTRAINT fk_dependency_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_dependency_depends_on FOREIGN KEY (depends_on_id) REFERENCES issues(id) ON DELETE CASCADE
);
