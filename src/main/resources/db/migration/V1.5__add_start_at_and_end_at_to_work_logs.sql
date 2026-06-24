-- Add start_at and end_at columns to work_logs
ALTER TABLE public.work_logs ADD COLUMN start_at TIMESTAMPTZ;
ALTER TABLE public.work_logs ADD COLUMN end_at TIMESTAMPTZ;
