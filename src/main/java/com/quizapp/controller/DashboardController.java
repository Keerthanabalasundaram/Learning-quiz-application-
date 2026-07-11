package com.quizapp.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quizapp.entity.Result;
import com.quizapp.entity.Subject;
import com.quizapp.entity.Unit;
import com.quizapp.entity.User;
import com.quizapp.repository.ResultRepository;
import com.quizapp.repository.SubjectRepository;
import com.quizapp.service.QuizService;
import com.quizapp.service.UserService;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(value = "subjectId", required = false) Long subjectId,
                            Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        String currentUsername = userDetails.getUsername();
        
        List<Subject> subjects = subjectRepository.findAll();
        Subject selectedSubject = null;
        List<Unit> units = new ArrayList<>();
        
        if (!subjects.isEmpty()) {
            if (subjectId != null) {
                final Long finalSubjectId = subjectId;
                selectedSubject = subjects.stream()
                        .filter(s -> s.getId().equals(finalSubjectId))
                        .findFirst()
                        .orElse(subjects.get(0));
            } else {
                selectedSubject = subjects.get(0);
            }
            units = quizService.getUnitsBySubjectId(selectedSubject.getId().intValue());
        }

        // Calculate lock status for each unit
        Map<Integer, Boolean> unitLockStatus = new HashMap<>();
        for (Unit unit : units) {
            User uploader = unit.getUploadedBy();
            String uploadedByUsername = (uploader != null) ? uploader.getUsername() : null;
            
            boolean isLocked = !unit.getIsCore() 
                && (uploadedByUsername == null || !currentUsername.equals(uploadedByUsername));
            
            unitLockStatus.put(unit.getId(), isLocked);
        }

        List<Result> results = resultRepository.findByUserOrderByCreatedAtDesc(user);

        // Calculate Stats
        int totalQuizzes = results.size();
        int passedQuizzes = 0;
        double totalAccuracy = 0.0;
        int totalTimeTaken = 0;

        for (Result r : results) {
            if (r.getPassed()) {
                passedQuizzes++;
            }
            totalAccuracy += r.getAccuracyPercentage().doubleValue();
            totalTimeTaken += r.getTimeTakenSeconds();
        }

        double avgAccuracy = totalQuizzes > 0 ? totalAccuracy / totalQuizzes : 0.0;

        // Custom Recommendations
        List<String> recommendations = new ArrayList<>();
        if (totalQuizzes == 0) {
            recommendations.add("👋 Welcome! Start by attempting Level 1 (Unit 1) to evaluate your baseline knowledge.");
        } else {
            if (avgAccuracy < 70.0) {
                recommendations.add("💡 Your average accuracy is below 70%. Focus on reviewing the study material of units you scored low on.");
            }
            if (user.getUnlockedLevel() == 1) {
                recommendations.add("📚 Try to clear Level 1: Fundamentals of Programming to unlock Object-Oriented Design.");
            }
            
            // Check specific unit weaknesses
            for (Unit unit : units) {
                double unitTotalAccuracy = 0.0;
                int unitCount = 0;
                for (Result r : results) {
                    if (r.getQuiz().getUnit().getId().equals(unit.getId())) {
                        unitTotalAccuracy += r.getAccuracyPercentage().doubleValue();
                        unitCount++;
                    }
                }
                if (unitCount > 0) {
                    double unitAvg = unitTotalAccuracy / unitCount;
                    if (unitAvg < 70.0) {
                        recommendations.add(String.format("⚠️ You are struggling with '%s' (average score %.1f%%). Ask the Chatbot Tutor for OOP or Syntax cheat sheets!", unit.getName(), unitAvg));
                    }
                }
            }
        }
        if (recommendations.isEmpty()) {
            recommendations.add("🎉 Excellent work! Keep pushing your limits. Try uploading your own study material to test yourself with AI questions!");
        }

        model.addAttribute("user", user);
        model.addAttribute("units", units);
        model.addAttribute("unitLockStatus", unitLockStatus);
        model.addAttribute("results", results);
        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("passedQuizzes", passedQuizzes);
        model.addAttribute("avgAccuracy", BigDecimal.valueOf(avgAccuracy).setScale(1, RoundingMode.HALF_UP));
        model.addAttribute("totalTimeTakenMinutes", totalTimeTaken / 60);
        model.addAttribute("recommendations", recommendations);

        return "dashboard";
    }
}