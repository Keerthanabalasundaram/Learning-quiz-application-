package com.quizapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;

    // Constructors
    public Subject() {}

    public Subject(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Builder
    public static SubjectBuilder builder() {
        return new SubjectBuilder();
    }

    public static class SubjectBuilder {
        private Long id;
        private String name;
        private String description;

        public SubjectBuilder id(Long id) { this.id = id; return this; }
        public SubjectBuilder name(String name) { this.name = name; return this; }
        public SubjectBuilder description(String description) { this.description = description; return this; }

        public Subject build() {
            return new Subject(id, name, description);
        }
    }
}