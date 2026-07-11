package com.quizapp.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quizapp.entity.Question;
import com.quizapp.entity.Quiz;
import com.quizapp.entity.Result;
import com.quizapp.entity.Unit;
import com.quizapp.entity.User;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.UnitRepository;

@Service
@Transactional
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserService userService;

    // A session state container to hold active quiz progression
    public static class QuizSessionState implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long quizId;
        private Integer unitId;
        private Integer levelNumber;
        private List<Long> askedQuestionIds = new ArrayList<>();
        private List<String> userAnswers = new ArrayList<>();
        private List<Boolean> answerOutcomes = new ArrayList<>(); // true = correct, false = wrong
        private int currentQuestionIndex = 0; // 0-based index
        private int correctAnswersCount = 0;
        private String currentDifficulty = "MEDIUM";
        private int totalQuestionsLimit = 10; // Default 10 questions
        private int timeLimitMinutes = 10;
        private Question currentQuestion;
        private long startTimeMs;
        private long questionStartTimeMs; // For tracking per-question timers

        // Manual Getters and Setters
        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
        
        public Integer getUnitId() { return unitId; }
        public void setUnitId(Integer unitId) { this.unitId = unitId; }
        
        public Integer getLevelNumber() { return levelNumber; }
        public void setLevelNumber(Integer levelNumber) { this.levelNumber = levelNumber; }
        
        public List<Long> getAskedQuestionIds() { return askedQuestionIds; }
        public void setAskedQuestionIds(List<Long> askedQuestionIds) { this.askedQuestionIds = askedQuestionIds; }
        
        public List<String> getUserAnswers() { return userAnswers; }
        public void setUserAnswers(List<String> userAnswers) { this.userAnswers = userAnswers; }
        
        public List<Boolean> getAnswerOutcomes() { return answerOutcomes; }
        public void setAnswerOutcomes(List<Boolean> answerOutcomes) { this.answerOutcomes = answerOutcomes; }
        
        public int getCurrentQuestionIndex() { return currentQuestionIndex; }
        public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
        
        public int getCorrectAnswersCount() { return correctAnswersCount; }
        public void setCorrectAnswersCount(int correctAnswersCount) { this.correctAnswersCount = correctAnswersCount; }
        
        public String getCurrentDifficulty() { return currentDifficulty; }
        public void setCurrentDifficulty(String currentDifficulty) { this.currentDifficulty = currentDifficulty; }
        
        public int getTotalQuestionsLimit() { return totalQuestionsLimit; }
        public void setTotalQuestionsLimit(int totalQuestionsLimit) { this.totalQuestionsLimit = totalQuestionsLimit; }
        
        public int getTimeLimitMinutes() { return timeLimitMinutes; }
        public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }
        
        public Question getCurrentQuestion() { return currentQuestion; }
        public void setCurrentQuestion(Question currentQuestion) { this.currentQuestion = currentQuestion; }
        
        public long getStartTimeMs() { return startTimeMs; }
        public void setStartTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; }
        
        public long getQuestionStartTimeMs() { return questionStartTimeMs; }
        public void setQuestionStartTimeMs(long questionStartTimeMs) { this.questionStartTimeMs = questionStartTimeMs; }
    }

    public List<Unit> getAllUnits() {
        return unitRepository.findAll();
    }

    public Unit getUnitById(Integer id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + id));
    }

    public Unit getUnitByLevel(Integer levelNumber) {
        return unitRepository.findByLevelNumber(levelNumber)
                .orElseThrow(() -> new IllegalArgumentException("Unit level " + levelNumber + " not found"));
    }

    @Transactional
    public QuizSessionState startQuiz(User user, Integer unitId) {
        Unit unit = getUnitById(unitId);

        // Progression check: ONLY for Java subject. Uploaded units skip this
        if (unit.getSubject() != null && "Java".equalsIgnoreCase(unit.getSubject().getName()) 
                && unit.getLevelNumber() > user.getUnlockedLevel()) {
            throw new IllegalStateException("Level " + unit.getLevelNumber() + " is locked! Pass previous levels first.");
        }

        // Deactivate any existing active quizzes for security
        Optional<Quiz> activeQuizOpt = quizRepository.findTopByUserAndStatusOrderByStartTimeDesc(user, "STARTED");
        activeQuizOpt.ifPresent(quiz -> {
            quiz.setStatus("EXPIRED");
            quiz.setEndTime(LocalDateTime.now());
            quizRepository.save(quiz);
        });

        // Create new Quiz entity
        Quiz quiz = Quiz.builder()
                .user(user)
                .unit(unit)
                .status("STARTED")
                .currentDifficulty("MEDIUM")
                .timeLimitMinutes(10) // 10 minutes global limit
                .build();
        quiz = quizRepository.save(quiz);

        // Prepare session state
        QuizSessionState state = new QuizSessionState();
        state.setQuizId(quiz.getId());
        state.setUnitId(unit.getId());
        state.setLevelNumber(unit.getLevelNumber());
        state.setStartTimeMs(System.currentTimeMillis());
        state.setTotalQuestionsLimit(10);
        state.setTimeLimitMinutes(quiz.getTimeLimitMinutes());

        // Fetch first question
        getNextAdaptiveQuestion(state);

        return state;
    }

    // NEW METHOD: For uploaded units - bypasses level lock
    @Transactional
    public QuizSessionState startQuizBypassLock(User user, Integer unitId) throws Exception {
        Unit unit = getUnitById(unitId);

        // NO progression check here - uploaded units are free to open

        // Deactivate any existing active quizzes
        Optional<Quiz> activeQuizOpt = quizRepository.findTopByUserAndStatusOrderByStartTimeDesc(user, "STARTED");
        activeQuizOpt.ifPresent(quiz -> {
            quiz.setStatus("EXPIRED");
            quiz.setEndTime(LocalDateTime.now());
            quizRepository.save(quiz);
        });

        // Create new Quiz entity
        Quiz quiz = Quiz.builder()
                .user(user)
                .unit(unit)
                .status("STARTED")
                .currentDifficulty("MEDIUM")
                .timeLimitMinutes(10)
                .build();
        quiz = quizRepository.save(quiz);

        // Prepare session state
        QuizSessionState state = new QuizSessionState();
        state.setQuizId(quiz.getId());
        state.setUnitId(unit.getId());
        state.setLevelNumber(unit.getLevelNumber());
        state.setStartTimeMs(System.currentTimeMillis());
        state.setTotalQuestionsLimit(10);
        state.setTimeLimitMinutes(quiz.getTimeLimitMinutes());

        // Fetch first question
        getNextAdaptiveQuestion(state);

        return state;
    }

    public void processAnswer(QuizSessionState state, String selectedOption) {
        Question currentQuestion = state.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }

        boolean isCorrect = currentQuestion.getCorrectAnswer().equalsIgnoreCase(selectedOption);
        state.getUserAnswers().add(selectedOption);
        state.getAnswerOutcomes().add(isCorrect);
        state.getAskedQuestionIds().add(currentQuestion.getId());

        if (isCorrect) {
            state.setCorrectAnswersCount(state.getCorrectAnswersCount() + 1);
            // Adaptive difficulty increase: EASY -> MEDIUM -> HARD
            if ("EASY".equals(state.getCurrentDifficulty())) {
                state.setCurrentDifficulty("MEDIUM");
            } else if ("MEDIUM".equals(state.getCurrentDifficulty())) {
                state.setCurrentDifficulty("HARD");
            }
        } else {
            // Adaptive difficulty decrease: HARD -> MEDIUM -> EASY
            if ("HARD".equals(state.getCurrentDifficulty())) {
                state.setCurrentDifficulty("MEDIUM");
            } else if ("MEDIUM".equals(state.getCurrentDifficulty())) {
                state.setCurrentDifficulty("EASY");
            }
        }

        // Update database quiz current difficulty state
        Quiz quiz = quizRepository.findById(state.getQuizId()).orElse(null);
        if (quiz != null) {
            quiz.setCurrentDifficulty(state.getCurrentDifficulty());
            quizRepository.save(quiz);
        }

        // Move to the next question
        state.setCurrentQuestionIndex(state.getCurrentQuestionIndex() + 1);
        
        if (state.getCurrentQuestionIndex() < state.getTotalQuestionsLimit()) {
            getNextAdaptiveQuestion(state);
        } else {
            state.setCurrentQuestion(null); // No more questions
        }
    }

    private void getNextAdaptiveQuestion(QuizSessionState state) {
        // Query for random questions of target difficulty in this unit
        List<Question> questions = questionRepository.findRandomByUnitAndDifficulty(state.getUnitId(), state.getCurrentDifficulty());
        
        // Filter out questions already asked
        Question nextQuestion = questions.stream()
                .filter(q -> !state.getAskedQuestionIds().contains(q.getId()))
                .findFirst()
                .orElse(null);

        // If no questions match target difficulty, fall back to ANY difficulty unasked
        if (nextQuestion == null) {
            List<Question> allUnitQuestions = questionRepository.findByUnit(getUnitById(state.getUnitId()));
            Collections.shuffle(allUnitQuestions);
            nextQuestion = allUnitQuestions.stream()
                    .filter(q -> !state.getAskedQuestionIds().contains(q.getId()))
                    .findFirst()
                    .orElse(null);
        }
         if (nextQuestion != null) {
            Hibernate.initialize(nextQuestion.getUnit());
        }

        state.setCurrentQuestion(nextQuestion);
        state.setQuestionStartTimeMs(System.currentTimeMillis());
    }

    @Transactional
    public Result submitQuiz(QuizSessionState state, User user, boolean timeExpired) {
        Quiz quiz = quizRepository.findById(state.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

        if (!"STARTED".equals(quiz.getStatus())) {
            throw new IllegalStateException("Quiz is already completed or expired.");
        }

        quiz.setStatus(timeExpired ? "EXPIRED" : "COMPLETED");
        quiz.setEndTime(LocalDateTime.now());
        quizRepository.save(quiz);
        
        int finalTotalQuestions = state.getTotalQuestionsLimit();
        int score = state.getCorrectAnswersCount();
        
        double accuracy = finalTotalQuestions > 0 ? ((double) score / finalTotalQuestions) * 100 : 0.0;
        int timeTaken = (int) ((System.currentTimeMillis() - state.getStartTimeMs()) / 1000);

        // Minimum threshold of 70% accuracy to pass
        boolean passed = accuracy >= 70.0;
        String feedbackMessage;
        if (passed) {
            feedbackMessage = "🎉 Excellent work! You passed Level " + state.getLevelNumber() + " with " + String.format("%.1f", accuracy) + "% accuracy!";
            // Unlock next level progression - ONLY for Java
            Unit unit = getUnitById(state.getUnitId());
            if(unit.getSubject() != null && "Java".equalsIgnoreCase(unit.getSubject().getName())) {
                userService.unlockNextLevel(user, state.getLevelNumber() + 1);
            }
        } else {
            feedbackMessage = "💪 Don't give up! Keep practicing. You scored " + String.format("%.1f", accuracy) + "%. Try again to unlock the next level!";
        }

        Result result = Result.builder()
                .quiz(quiz)
                .user(user)
                .score(score)
                .totalQuestions(finalTotalQuestions)
                .accuracyPercentage(BigDecimal.valueOf(accuracy))
                .timeTakenSeconds(timeTaken)
                .passed(passed)
                .feedbackMessage(feedbackMessage)
                .build();

        return resultRepository.save(result);
    }
    
    public List<Unit> getUnitsBySubjectId(Integer subjectId) {
        return unitRepository.findBySubject_Id(subjectId);
    }
}