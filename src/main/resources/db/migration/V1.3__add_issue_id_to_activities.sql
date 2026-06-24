ALTER TABLE public.activities ADD COLUMN issue_id uuid;

ALTER TABLE public.activities
    ADD CONSTRAINT fk_activities_issue FOREIGN KEY (issue_id) REFERENCES public.issues(id) ON DELETE CASCADE;

CREATE INDEX idx_activities_issue_id ON public.activities(issue_id);
