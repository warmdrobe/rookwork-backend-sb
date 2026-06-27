-- V2.8__add_foreign_key_indexes.sql
--
-- Adds missing indexes for all frequently-queried foreign key columns.
-- PostgreSQL creates indexes automatically only for PRIMARY KEY constraints;
-- foreign key columns require explicit CREATE INDEX for efficient JOIN/WHERE lookups.
--
-- All indexes are created with IF NOT EXISTS so this migration is safe to replay.
-- On a live production database with large tables, replace CREATE INDEX with
-- CREATE INDEX CONCURRENTLY (requires running outside a transaction block).
--

-- ──────────────────────────────────────────────────────────────────────────────
-- issues
-- ──────────────────────────────────────────────────────────────────────────────

-- Most queried: getAllIssues, getIssuesByProject
CREATE INDEX IF NOT EXISTS idx_issues_project_id  ON public.issues (project_id);

-- Used in parent-child subtask hierarchy queries
CREATE INDEX IF NOT EXISTS idx_issues_parent_id   ON public.issues (parent_id);

-- Used when filtering by creator
CREATE INDEX IF NOT EXISTS idx_issues_created_by  ON public.issues (created_by);

-- ──────────────────────────────────────────────────────────────────────────────
-- issue_assignees  (join table created in V1.4)
-- ──────────────────────────────────────────────────────────────────────────────

-- Lookup: which users are assigned to a given issue
CREATE INDEX IF NOT EXISTS idx_issue_assignees_issue_id ON public.issue_assignees (issue_id);

-- Lookup: which issues are assigned to a given user (MyIssuesPage, Dashboard)
CREATE INDEX IF NOT EXISTS idx_issue_assignees_user_id  ON public.issue_assignees (user_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- comments
-- ──────────────────────────────────────────────────────────────────────────────

-- getAllCommentByIssueId
CREATE INDEX IF NOT EXISTS idx_comments_issue_id        ON public.comments (issue_id);

-- Thread replies
CREATE INDEX IF NOT EXISTS idx_comments_parent_id       ON public.comments (parent_comment_id);

-- Author lookup
CREATE INDEX IF NOT EXISTS idx_comments_user_id         ON public.comments (user_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- activities
-- ──────────────────────────────────────────────────────────────────────────────

-- getActivitiesByProject
CREATE INDEX IF NOT EXISTS idx_activities_project_id    ON public.activities (project_id);

-- getActivitiesByIssue (used in issue detail page)
CREATE INDEX IF NOT EXISTS idx_activities_issue_id      ON public.activities (entity_id);

-- Actor-based audit queries
CREATE INDEX IF NOT EXISTS idx_activities_actor_id      ON public.activities (actor_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- notifications
-- ──────────────────────────────────────────────────────────────────────────────

-- getNotificationsByUser – this is the hottest query on the notifications table
CREATE INDEX IF NOT EXISTS idx_notifications_user_id    ON public.notifications (user_id);

-- Filter unread notifications
CREATE INDEX IF NOT EXISTS idx_notifications_is_read    ON public.notifications (user_id, is_read);

-- ──────────────────────────────────────────────────────────────────────────────
-- project_members  (composite PK is already indexed; add individual cols for queries)
-- ──────────────────────────────────────────────────────────────────────────────

-- findAllByProject_Id (load members for a project)
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON public.project_members (project_id);

-- findAllByUser_Id (my projects list)
CREATE INDEX IF NOT EXISTS idx_project_members_user_id    ON public.project_members (user_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- invitations
-- ──────────────────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_invitations_project_id   ON public.invitations (project_id);
CREATE INDEX IF NOT EXISTS idx_invitations_invited_user ON public.invitations (invited_user);
CREATE INDEX IF NOT EXISTS idx_invitations_invited_by   ON public.invitations (invited_by);

-- ──────────────────────────────────────────────────────────────────────────────
-- subtasks
-- ──────────────────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_subtasks_issue_id        ON public.subtasks (issue_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- work_logs (if the table exists – added in V1.5)
-- ──────────────────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_work_logs_issue_id       ON public.work_logs (issue_id);
CREATE INDEX IF NOT EXISTS idx_work_logs_user_id        ON public.work_logs (user_id);
