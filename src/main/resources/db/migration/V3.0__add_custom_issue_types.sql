CREATE TABLE public.issue_types (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_key VARCHAR(100) NOT NULL,
    color VARCHAR(20) NOT NULL,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_issue_types_project FOREIGN KEY (project_id) REFERENCES public.projects(id) ON DELETE CASCADE,
    CONSTRAINT unique_project_issue_type_name UNIQUE (project_id, name)
);

-- Seed system issue types for existing projects
INSERT INTO public.issue_types (id, project_id, name, description, icon_key, color, is_system, created_at, updated_at)
SELECT gen_random_uuid(), id, 'TASK', 'A single piece of work', 'task', '#1D4ED8', TRUE, NOW(), NOW() FROM public.projects;

INSERT INTO public.issue_types (id, project_id, name, description, icon_key, color, is_system, created_at, updated_at)
SELECT gen_random_uuid(), id, 'STORY', 'A user-facing feature', 'story', '#15803D', TRUE, NOW(), NOW() FROM public.projects;

INSERT INTO public.issue_types (id, project_id, name, description, icon_key, color, is_system, created_at, updated_at)
SELECT gen_random_uuid(), id, 'EPIC', 'A large body of work', 'epic', '#7E22CE', TRUE, NOW(), NOW() FROM public.projects;

-- Add issue_type_id column to issues table
ALTER TABLE public.issues ADD COLUMN issue_type_id UUID;

-- Update issues to point to the correct issue type
UPDATE public.issues i
SET issue_type_id = it.id
FROM public.issue_types it
WHERE i.project_id = it.project_id AND i.issue_type = it.name;

-- Set issue_type_id to NOT NULL and add foreign key
ALTER TABLE public.issues ALTER COLUMN issue_type_id SET NOT NULL;
ALTER TABLE public.issues DROP CONSTRAINT IF EXISTS issues_issue_type_check;
ALTER TABLE public.issues DROP COLUMN issue_type;
ALTER TABLE public.issues ADD CONSTRAINT fk_issues_issue_type FOREIGN KEY (issue_type_id) REFERENCES public.issue_types(id);
