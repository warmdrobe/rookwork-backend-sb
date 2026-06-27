ALTER TABLE public.events
ADD COLUMN location character varying(255),
ADD COLUMN color character varying(50),
ADD COLUMN start_time timestamp(6) without time zone,
ADD COLUMN end_time timestamp(6) without time zone;

-- Migrate deadline values to start_time for any pre-existing rows
UPDATE public.events SET start_time = deadline WHERE start_time IS NULL;

CREATE TABLE public.event_guests (
    event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_event_guests_event FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_guests_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
