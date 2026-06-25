-- Add issue_id to files to link uploaded files to issues as attachments (Idempotent script)
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS issue_id uuid;

-- Add foreign key constraint if it does not exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_files_issues'
    ) THEN
        ALTER TABLE public.files ADD CONSTRAINT fk_files_issues FOREIGN KEY (issue_id) REFERENCES public.issues(id) ON DELETE CASCADE;
    END IF;
END $$;
