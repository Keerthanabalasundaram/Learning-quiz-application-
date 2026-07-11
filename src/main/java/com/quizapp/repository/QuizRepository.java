package com.quizapp.repository;

import com.quizapp.entity.Quiz;
import com.quizapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByUserOrderByStartTimeDesc(User user);
    Optional<Quiz> findTopByUserAndStatusOrderByStartTimeDesc(User user, String status);
}
