package com.example.rookwork_backend_sb.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name="files")
public class File {
    @Id
    @Column(name="id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name ="original_name")
    private String originalName;

    @Column(name="stored_name")
    private String storedName;

    @Column(name="mime_type")
    private String mimeType;

    @Column(name="size_bytes")
    private Integer sizeBytes;

    @Column(name="storage_path")
    private String storagePath;

    @Column(name="uploaded_by")
    private String uploadedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

}
