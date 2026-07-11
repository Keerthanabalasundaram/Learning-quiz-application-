package com.quizapp.repository;

import com.quizapp.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Integer> {
    Optional<Unit> findByLevelNumber(Integer levelNumber);
    List<Unit> findBySubject_Id(Integer subjectId);
}

