-- V1.4__add_multi_assignees_to_issues.sql
-- Chuyen tu single assignee (issues.assigned_to) sang multi-assignee (issue_assignees join table)

-- 1. Tao bang join issue_assignees
CREATE TABLE public.issue_assignees (
    issue_id uuid NOT NULL,
    user_id  uuid NOT NULL,
    CONSTRAINT pk_issue_assignees PRIMARY KEY (issue_id, user_id),
    CONSTRAINT fk_issue_assignees_issue
        FOREIGN KEY (issue_id) REFERENCES public.issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_assignees_user
        FOREIGN KEY (user_id)  REFERENCES public.users(id)  ON DELETE CASCADE
);

-- 2. Migrate data: copy ban ghi tu cot assigned_to sang bang moi
INSERT INTO public.issue_assignees (issue_id, user_id)
SELECT id, assigned_to
FROM public.issues
WHERE assigned_to IS NOT NULL;

-- 3. Xoa cot assigned_to cu
ALTER TABLE public.issues DROP COLUMN IF EXISTS assigned_to;
