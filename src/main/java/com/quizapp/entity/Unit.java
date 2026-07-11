package com.quizapp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "units")
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber; // Level 1 -> Unit 1, Level 2 -> Unit 2, etc.

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // NEW 1: To know if this is core Java or user uploaded
    @Column(name = "is_core", nullable = false)
    private Boolean isCore = true; // true = Inbuilt Java, false = Uploaded by user

    // NEW 2: To track who uploaded it. null for core units
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Unit() {}

    public Unit(Integer id, String name, Integer levelNumber, String description, Subject subject, Boolean isCore, User uploadedBy, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.levelNumber = levelNumber;
        this.description = description;
        this.subject = subject;
        this.isCore = isCore;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevelNumber() { return levelNumber; }
    public void setLevelNumber(Integer levelNumber) { this.levelNumber = levelNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // NEW Getters/Setters
    public Boolean getIsCore() { return isCore; }
    public void setIsCore(Boolean isCore) { this.isCore = isCore; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    // Builder
    public static UnitBuilder builder() {
        return new UnitBuilder();
    }

    public static class UnitBuilder {
        private Integer id;
        private String name;
        private Integer levelNumber;
        private String description;
        private Subject subject;
        private Boolean isCore = true;
        private User uploadedBy;
        private LocalDateTime createdAt;

        public UnitBuilder id(Integer id) { this.id = id; return this; }
        public UnitBuilder name(String name) { this.name = name; return this; }
        public UnitBuilder levelNumber(Integer levelNumber) { this.levelNumber = levelNumber; return this; }
        public UnitBuilder description(String description) { this.description = description; return this; }
        public UnitBuilder subject(Subject subject) { this.subject = subject; return this; }
        public UnitBuilder isCore(Boolean isCore) { this.isCore = isCore; return this; } // NEW
        public UnitBuilder uploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; return this; } // NEW
        public UnitBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Unit build() {
            return new Unit(id, name, levelNumber, description, subject, isCore, uploadedBy, createdAt);
        }
    }
}