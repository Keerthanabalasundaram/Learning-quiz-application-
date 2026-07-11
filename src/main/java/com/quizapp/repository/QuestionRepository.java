package com.quizapp.repository;

import com.quizapp.entity.Question;
import com.quizapp.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByUnitAndDifficulty(Unit unit, String difficulty);
    List<Question> findByUnit(Unit unit);
    
    @Query("SELECT q FROM Question q WHERE q.unit.id = :unitId AND q.difficulty = :difficulty ORDER BY FUNCTION('RAND')")
    List<Question> findRandomByUnitAndDifficulty(@Param("unitId") Integer unitId, @Param("difficulty") String difficulty);
}
