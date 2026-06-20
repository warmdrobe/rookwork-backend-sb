--
-- PostgreSQL database dump
--

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activities (
    id uuid NOT NULL,
    action character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    entity_id uuid NOT NULL,
    entity_name character varying(255),
    entity_type character varying(255) NOT NULL,
    metadata jsonb,
    actor_id uuid NOT NULL,
    project_id uuid NOT NULL,
    CONSTRAINT activities_action_check CHECK (((action)::text = ANY ((ARRAY['CREATED'::character varying, 'COMPLETED'::character varying, 'MOVED'::character varying, 'COMMENTED'::character varying, 'UPLOADED'::character varying, 'ASSIGNED'::character varying, 'UPDATED'::character varying, 'DELETED'::character varying])::text[]))),
    CONSTRAINT activities_entity_type_check CHECK (((entity_type)::text = ANY ((ARRAY['ISSUE'::character varying, 'COMMENT'::character varying, 'FILE'::character varying, 'SUBTASK'::character varying])::text[])))
);


--
-- Name: comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comments (
    id uuid NOT NULL,
    content character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    issue_id uuid NOT NULL,
    parent_comment_id uuid,
    user_id uuid NOT NULL
);


--
-- Name: events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.events (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    deadline timestamp(6) without time zone,
    event_description character varying(255),
    event_name character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone,
    project_id uuid,
    user_id uuid
);


--
-- Name: files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.files (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    mime_type character varying(255),
    original_name character varying(255),
    size_bytes integer,
    storage_path character varying(255),
    stored_name character varying(255),
    updated_at timestamp(6) without time zone,
    uploaded_by character varying(255),
    user_id uuid NOT NULL
);


--
-- Name: invitations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invitations (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    invited_by uuid NOT NULL,
    invited_user uuid NOT NULL,
    project_id uuid NOT NULL,
    CONSTRAINT invitations_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'DECLINED'::character varying])::text[])))
);


--
-- Name: issues; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.issues (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    deadline timestamp(6) without time zone,
    description text,
    issue_name character varying(200) NOT NULL,
    issue_type character varying(20) NOT NULL,
    priority character varying(255),
    status character varying(255),
    updated_at timestamp(6) without time zone,
    assigned_to uuid,
    created_by uuid,
    parent_id uuid,
    project_id uuid NOT NULL,
    CONSTRAINT issues_issue_type_check CHECK (((issue_type)::text = ANY ((ARRAY['EPIC'::character varying, 'STORY'::character varying, 'TASK'::character varying])::text[]))),
    CONSTRAINT issues_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'URGENT'::character varying])::text[]))),
    CONSTRAINT issues_status_check CHECK (((status)::text = ANY ((ARRAY['TO_DO'::character varying, 'IN_PROGRESS'::character varying, 'DONE'::character varying])::text[])))
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    is_read boolean,
    message text,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    invitation_id uuid,
    issue_id uuid,
    sender_id uuid,
    user_id uuid NOT NULL
);


--
-- Name: project_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_members (
    created_at timestamp(6) without time zone,
    role character varying(255),
    updated_at timestamp(6) without time zone,
    project_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT project_members_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'CONTRIBUTOR'::character varying])::text[])))
);


--
-- Name: projects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.projects (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    is_private boolean,
    project_name character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: subtasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subtasks (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    is_done boolean,
    subtask_description character varying(255),
    subtask_name character varying(255),
    updated_at timestamp(6) without time zone,
    issue_id uuid NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    is_active boolean,
    is_verified boolean,
    password_hash character varying(255),
    picture character varying(255),
    profile_name character varying(255),
    refresh_token_expires_at timestamp(6) without time zone,
    refresh_token_hash character varying(255),
    updated_at timestamp(6) without time zone
);


--
-- Name: work_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_logs (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    hours numeric(5,2) NOT NULL,
    logged_at timestamp(6) without time zone NOT NULL,
    note text,
    issue_id uuid NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: activities activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT activities_pkey PRIMARY KEY (id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: files files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_pkey PRIMARY KEY (id);


--
-- Name: invitations invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_pkey PRIMARY KEY (id);


--
-- Name: issues issues_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.issues
    ADD CONSTRAINT issues_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: project_members project_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT project_members_pkey PRIMARY KEY (project_id, user_id);


--
-- Name: projects projects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);


--
-- Name: subtasks subtasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subtasks
    ADD CONSTRAINT subtasks_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: work_logs work_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_logs
    ADD CONSTRAINT work_logs_pkey PRIMARY KEY (id);


--
-- Name: notifications fk13vcnq3ukas06ho1yrbc5lrb5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk13vcnq3ukas06ho1yrbc5lrb5 FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- Name: comments fk287j1dpionjmfs2yycfjmy5j2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk287j1dpionjmfs2yycfjmy5j2 FOREIGN KEY (issue_id) REFERENCES public.issues(id);


--
-- Name: issues fk4j2x3reshuu7qj5svh6eacnpt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.issues
    ADD CONSTRAINT fk4j2x3reshuu7qj5svh6eacnpt FOREIGN KEY (project_id) REFERENCES public.projects(id);


--
-- Name: issues fk5bf1viph0f0wa99esuvbc0895; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.issues
    ADD CONSTRAINT fk5bf1viph0f0wa99esuvbc0895 FOREIGN KEY (assigned_to) REFERENCES public.users(id);


--
-- Name: comments fk7h839m3lkvhbyv3bcdv7sm4fj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk7h839m3lkvhbyv3bcdv7sm4fj FOREIGN KEY (parent_comment_id) REFERENCES public.comments(id);


--
-- Name: comments fk8omq0tc18jd43bu5tjh6jvraq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk8omq0tc18jd43bu5tjh6jvraq FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: subtasks fk9sxswwj59ri10t21cr8l34lxq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subtasks
    ADD CONSTRAINT fk9sxswwj59ri10t21cr8l34lxq FOREIGN KEY (issue_id) REFERENCES public.issues(id);


--
-- Name: notifications fk9y21adhxn0ayjhfocscqox7bh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: events fkat8p3s7yjcp57lny4udqvqncq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fkat8p3s7yjcp57lny4udqvqncq FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: issues fkaxw1cvm6o4r7vdednj01v82x6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.issues
    ADD CONSTRAINT fkaxw1cvm6o4r7vdednj01v82x6 FOREIGN KEY (parent_id) REFERENCES public.issues(id);


--
-- Name: events fkccfs1y85nru2df6x7pi8bk77n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fkccfs1y85nru2df6x7pi8bk77n FOREIGN KEY (project_id) REFERENCES public.projects(id);


--
-- Name: files fkdgr5hx49828s5vhjo1s8q3wdp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT fkdgr5hx49828s5vhjo1s8q3wdp FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: project_members fkdki1sp2homqsdcvqm9yrix31g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT fkdki1sp2homqsdcvqm9yrix31g FOREIGN KEY (project_id) REFERENCES public.projects(id);


--
-- Name: issues fkeytvklidnnq8cnpeybixvy9rv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.issues
    ADD CONSTRAINT fkeytvklidnnq8cnpeybixvy9rv FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: project_members fkgul2el0qjk5lsvig3wgajwm77; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT fkgul2el0qjk5lsvig3wgajwm77 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: invitations fkh67axu8o0vump4ii8d89e2244; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT fkh67axu8o0vump4ii8d89e2244 FOREIGN KEY (invited_by) REFERENCES public.users(id);


--
-- Name: notifications fkjno6gt1uc7lm4vql15yoy87hk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkjno6gt1uc7lm4vql15yoy87hk FOREIGN KEY (invitation_id) REFERENCES public.invitations(id);


--
-- Name: activities fkmfjrc8jdvy0yrr7x67qmrxkue; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT fkmfjrc8jdvy0yrr7x67qmrxkue FOREIGN KEY (actor_id) REFERENCES public.users(id);


--
-- Name: notifications fkmjyld6b6mv3pt4oq9bx79pesd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkmjyld6b6mv3pt4oq9bx79pesd FOREIGN KEY (issue_id) REFERENCES public.issues(id);


--
-- Name: invitations fknap2cfwq9dt829bsasj8qpblf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT fknap2cfwq9dt829bsasj8qpblf FOREIGN KEY (project_id) REFERENCES public.projects(id);


--
-- Name: invitations fkogyxyjhkomc7dr7wh4kv60egj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT fkogyxyjhkomc7dr7wh4kv60egj FOREIGN KEY (invited_user) REFERENCES public.users(id);


--
-- Name: work_logs fkow2mxee07ckvecl4jiemar1dn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_logs
    ADD CONSTRAINT fkow2mxee07ckvecl4jiemar1dn FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: activities fksp1gle1x16hi1viq0vjx26hmf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT fksp1gle1x16hi1viq0vjx26hmf FOREIGN KEY (project_id) REFERENCES public.projects(id);


--
-- Name: work_logs fktqf9kb2uph1ny1gnt30x53kcq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_logs
    ADD CONSTRAINT fktqf9kb2uph1ny1gnt30x53kcq FOREIGN KEY (issue_id) REFERENCES public.issues(id);


--
-- PostgreSQL database dump complete
--


