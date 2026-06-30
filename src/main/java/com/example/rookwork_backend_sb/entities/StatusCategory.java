package com.example.rookwork_backend_sb.entities;

/**
 * Semantic category for a ProjectStatus.
 * Used for progress reporting and completion counting regardless of
 * the custom display name chosen by the project owner.
 */
public enum StatusCategory {
    TO_DO,
    IN_PROGRESS,
    DONE
}
