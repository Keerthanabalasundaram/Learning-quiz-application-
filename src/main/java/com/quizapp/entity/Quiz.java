package com.quizapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false, length = 20)
    private String status; // 'STARTED', 'COMPLETED', 'EXPIRED'

    @Column(name = "current_difficulty", nullable = false, length = 20)
    private String currentDifficulty; // 'EASY', 'MEDIUM', 'HARD'

    @Column(name = "start_time", updatable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes = 15;

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
        if (status == null) {
            status = "STARTED";
        }
        if (currentDifficulty == null) {
            currentDifficulty = "MEDIUM";
        }
    }

    // Constructors
    public Quiz() {}

    public Quiz(Long id, User user, Unit unit, String status, String currentDifficulty, LocalDateTime startTime, LocalDateTime endTime, Integer timeLimitMinutes) {
        this.id = id;
        this.user = user;
        this.unit = unit;
        this.status = status;
        this.currentDifficulty = currentDifficulty;
        this.startTime = startTime;
        this.endTime = endTime;
        if (timeLimitMinutes != null) {
            this.timeLimitMinutes = timeLimitMinutes;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(String currentDifficulty) { this.currentDifficulty = currentDifficulty; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    // Builder
    public static QuizBuilder builder() {
        return new QuizBuilder();
    }

    public static class QuizBuilder {
        private Long id;
        private User user;
        private Unit unit;
        private String status;
        private String currentDifficulty;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer timeLimitMinutes = 15;

        public QuizBuilder id(Long id) { this.id = id; return this; }
        public QuizBuilder user(User user) { this.user = user; return this; }
        public QuizBuilder unit(Unit unit) { this.unit = unit; return this; }
        public QuizBuilder status(String status) { this.status = status; return this; }
        public QuizBuilder currentDifficulty(String currentDifficulty) { this.currentDifficulty = currentDifficulty; return this; }
        public QuizBuilder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public QuizBuilder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public QuizBuilder timeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; return this; }

        public Quiz build() {
            return new Quiz(id, user, unit, status, currentDifficulty, startTime, endTime, timeLimitMinutes);
        }
    }
}
