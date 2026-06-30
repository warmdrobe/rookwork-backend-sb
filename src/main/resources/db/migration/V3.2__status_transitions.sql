CREATE TABLE status_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    from_status_id UUID NOT NULL REFERENCES project_statuses(id) ON DELETE CASCADE,
    to_status_id UUID NOT NULL REFERENCES project_statuses(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_transition UNIQUE (project_id, from_status_id, to_status_id),
    CONSTRAINT no_self_loop CHECK (from_status_id <> to_status_id)
);

CREATE INDEX idx_transitions_project ON status_transitions(project_id);
CREATE INDEX idx_transitions_from ON status_transitions(from_status_id);
