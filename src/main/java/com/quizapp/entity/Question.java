package com.quizapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id")
    private Upload upload; // Null for inbuilt questions

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false, length = 255)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 255)
    private String optionB;

    @Column(name = "option_c", nullable = false, length = 255)
    private String optionC;

    @Column(name = "option_d", nullable = false, length = 255)
    private String optionD;

    @Column(name = "correct_answer", nullable = false, length = 1)
    private String correctAnswer; // 'A', 'B', 'C', or 'D'

    @Column(nullable = false, length = 20)
    private String difficulty; // 'EASY', 'MEDIUM', 'HARD'

    @Column(name = "is_inbuilt")
    private Boolean isInbuilt = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Question() {}

    public Question(Long id, Unit unit, Upload upload, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String difficulty, Boolean isInbuilt, LocalDateTime createdAt) {
        this.id = id;
        this.unit = unit;
        this.upload = upload;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.difficulty = difficulty;
        if (isInbuilt != null) {
            this.isInbuilt = isInbuilt;
        }
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Boolean getIsInbuilt() { return isInbuilt; }
    public void setIsInbuilt(Boolean isInbuilt) { this.isInbuilt = isInbuilt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder
    public static QuestionBuilder builder() {
        return new QuestionBuilder();
    }

    public static class QuestionBuilder {
        private Long id;
        private Unit unit;
        private Upload upload;
        private String questionText;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String difficulty;
        private Boolean isInbuilt = true;
        private LocalDateTime createdAt;

        public QuestionBuilder id(Long id) { this.id = id; return this; }
        public QuestionBuilder unit(Unit unit) { this.unit = unit; return this; }
        public QuestionBuilder upload(Upload upload) { this.upload = upload; return this; }
        public QuestionBuilder questionText(String questionText) { this.questionText = questionText; return this; }
        public QuestionBuilder optionA(String optionA) { this.optionA = optionA; return this; }
        public QuestionBuilder optionB(String optionB) { this.optionB = optionB; return this; }
        public QuestionBuilder optionC(String optionC) { this.optionC = optionC; return this; }
        public QuestionBuilder optionD(String optionD) { this.optionD = optionD; return this; }
        public QuestionBuilder correctAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; return this; }
        public QuestionBuilder difficulty(String difficulty) { this.difficulty = difficulty; return this; }
        public QuestionBuilder isInbuilt(Boolean isInbuilt) { this.isInbuilt = isInbuilt; return this; }
        public QuestionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Question build() {
            return new Question(id, unit, upload, questionText, optionA, optionB, optionC, optionD, correctAnswer, difficulty, isInbuilt, createdAt);
        }
    }
}
