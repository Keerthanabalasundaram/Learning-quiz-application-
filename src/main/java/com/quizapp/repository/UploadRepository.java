package com.quizapp.repository;

import com.quizapp.entity.Upload;
import com.quizapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<Upload, Long> {
    List<Upload> findByUserOrderByUploadTimeDesc(User user);
}
