package com.quizapp.entity;

import java.math.BigDecimal;
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
@Table(name = "results")
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "accuracy_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Column(name = "time_taken_seconds", nullable = false)
    private Integer timeTakenSeconds;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "feedback_message", nullable = false)
    private String feedbackMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Result() {}

    public Result(Long id, Quiz quiz, User user, Integer score, Integer totalQuestions, BigDecimal accuracyPercentage, Integer timeTakenSeconds, Boolean passed, String feedbackMessage, LocalDateTime createdAt) {
        this.id = id;
        this.quiz = quiz;
        this.user = user;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.accuracyPercentage = accuracyPercentage;
        this.timeTakenSeconds = timeTakenSeconds;
        this.passed = passed;
        this.feedbackMessage = feedbackMessage;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    public BigDecimal getAccuracyPercentage() { return accuracyPercentage; }
    public void setAccuracyPercentage(BigDecimal accuracyPercentage) { this.accuracyPercentage = accuracyPercentage; }
    public Integer getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(Integer timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public String getFeedbackMessage() { return feedbackMessage; }
    public void setFeedbackMessage(String feedbackMessage) { this.feedbackMessage = feedbackMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder
    public static ResultBuilder builder() {
        return new ResultBuilder();
    }

    public static class ResultBuilder {
        private Long id;
        private Quiz quiz;
        private User user;
        private Integer score;
        private Integer totalQuestions;
        private BigDecimal accuracyPercentage;
        private Integer timeTakenSeconds;
        private Boolean passed;
        private String feedbackMessage;
        private LocalDateTime createdAt;

        public ResultBuilder id(Long id) { this.id = id; return this; }
        public ResultBuilder quiz(Quiz quiz) { this.quiz = quiz; return this; }
        public ResultBuilder user(User user) { this.user = user; return this; }
        public ResultBuilder score(Integer score) { this.score = score; return this; }
        public ResultBuilder totalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; return this; }
        public ResultBuilder accuracyPercentage(BigDecimal accuracyPercentage) { this.accuracyPercentage = accuracyPercentage; return this; }
        public ResultBuilder timeTakenSeconds(Integer timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; return this; }
        public ResultBuilder passed(Boolean passed) { this.passed = passed; return this; }
        public ResultBuilder feedbackMessage(String feedbackMessage) { this.feedbackMessage = feedbackMessage; return this; }
        public ResultBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Result build() {
            return new Result(id, quiz, user, score, totalQuestions, accuracyPercentage, timeTakenSeconds, passed, feedbackMessage, createdAt);
        }
    }
}
