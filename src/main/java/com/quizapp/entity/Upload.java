package com.quizapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploads")
public class Upload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType; // 'CSV', 'TXT'

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_time", updatable = false)
    private LocalDateTime uploadTime;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }

    // Constructors
    public Upload() {}

    public Upload(Long id, User user, String fileName, String fileType, Long fileSize, LocalDateTime uploadTime) {
        this.id = id;
        this.user = user;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    // Builder
    public static UploadBuilder builder() {
        return new UploadBuilder();
    }

    public static class UploadBuilder {
        private Long id;
        private User user;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private LocalDateTime uploadTime;

        public UploadBuilder id(Long id) { this.id = id; return this; }
        public UploadBuilder user(User user) { this.user = user; return this; }
        public UploadBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public UploadBuilder fileType(String fileType) { this.fileType = fileType; return this; }
        public UploadBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public UploadBuilder uploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; return this; }

        public Upload build() {
            return new Upload(id, user, fileName, fileType, fileSize, uploadTime);
        }
    }
}
