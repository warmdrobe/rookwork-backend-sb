-- Name: comment_reactions; Type: TABLE; Schema: public
-- Lưu trữ biểu cảm (reactions) của người dùng trên các bình luận.
-- Mỗi người dùng chỉ có tối đa một loại reaction trên một bình luận (unique constraint).

CREATE TABLE public.comment_reactions (
    id             uuid                        NOT NULL,
    comment_id     uuid                        NOT NULL,
    user_id        uuid                        NOT NULL,
    reaction_type  character varying(50)       NOT NULL,
    created_at     TIMESTAMPTZ
);

ALTER TABLE ONLY public.comment_reactions
    ADD CONSTRAINT comment_reactions_pkey PRIMARY KEY (id);

-- Một user chỉ có tối đa 1 reaction per comment
ALTER TABLE ONLY public.comment_reactions
    ADD CONSTRAINT uq_comment_reactions_comment_user UNIQUE (comment_id, user_id);

-- FK đến comments
ALTER TABLE ONLY public.comment_reactions
    ADD CONSTRAINT fk_comment_reactions_comment
    FOREIGN KEY (comment_id) REFERENCES public.comments(id) ON DELETE CASCADE;

-- FK đến users
ALTER TABLE ONLY public.comment_reactions
    ADD CONSTRAINT fk_comment_reactions_user
    FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

-- Index để query nhanh theo comment_id
CREATE INDEX idx_comment_reactions_comment_id ON public.comment_reactions (comment_id);
CREATE INDEX idx_comment_reactions_user_id    ON public.comment_reactions (user_id);
