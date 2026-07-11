package com.quizapp.controller;

import com.quizapp.entity.*;
import com.quizapp.service.QuizService;
import com.quizapp.service.UserService;
import com.quizapp.repository.UnitRepository; // ADD THIS
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/quiz")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserService userService;

    @Autowired
    private UnitRepository unitRepository; // ADD THIS

    @GetMapping("/start")
    public String startQuiz(@RequestParam("unitId") Integer unitId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            HttpSession session) {
        User user = userService.findByUsername(userDetails.getUsername());
        
        Unit unit = unitRepository.findById(unitId).orElse(null); // ADD THIS

        try {
            // NEW RULE: Uploaded units can be opened by anyone who sees them unlocked
            // Core units 1-4 still need to follow currentUnitId order
            if(unit != null && unit.getName().contains("NOTES")) { 
                // This is an uploaded unit - bypass the normal level check
                QuizService.QuizSessionState state = quizService.startQuizBypassLock(user, unitId);
                session.setAttribute("quizState", state);
                return "redirect:/quiz/question";
            } else {
                // This is a core Java unit - keep normal level check
                QuizService.QuizSessionState state = quizService.startQuiz(user, unitId);
                session.setAttribute("quizState", state);
                return "redirect:/quiz/question";
            }
            
        } catch (Exception e) {
            return "redirect:/dashboard?error=" + e.getMessage();
        }
    }

    @GetMapping("/question")
    public String showQuestion(HttpSession session, Model model) {
        QuizService.QuizSessionState state = (QuizService.QuizSessionState) session.getAttribute("quizState");
        if (state == null) {
            return "redirect:/dashboard";
        }

        if (state.getCurrentQuestion() == null) {
            return "redirect:/quiz/complete";
        }

        long timeElapsed = (System.currentTimeMillis() - state.getStartTimeMs()) / 1000;
        long remainingGlobalSeconds = (state.getTimeLimitMinutes() * 60) - timeElapsed;

        if (remainingGlobalSeconds <= 0) {
            return "redirect:/quiz/complete?timeout=true";
        }

        model.addAttribute("state", state);
        model.addAttribute("question", state.getCurrentQuestion());
        model.addAttribute("globalRemainingSeconds", remainingGlobalSeconds);
        model.addAttribute("questionLimitSeconds", 45);

        return "quiz";
    }

    @PostMapping("/submit")
    public String submitAnswer(@RequestParam(value = "selectedOption", required = false) String selectedOption,
                               HttpSession session) {
        QuizService.QuizSessionState state = (QuizService.QuizSessionState) session.getAttribute("quizState");
        if (state == null) {
            return "redirect:/dashboard";
        }

        String ans = (selectedOption != null) ? selectedOption : "";
        quizService.processAnswer(state, ans);

        if (state.getCurrentQuestion() == null) {
            return "redirect:/quiz/complete";
        }

        return "redirect:/quiz/question";
    }

    @GetMapping("/complete")
    public String completeQuiz(@RequestParam(value = "timeout", required = false) Boolean timeout,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpSession session,
                               Model model) {
        QuizService.QuizSessionState state = (QuizService.QuizSessionState) session.getAttribute("quizState");
        if (state == null) {
            return "redirect:/dashboard";
        }

        User user = userService.findByUsername(userDetails.getUsername());
        boolean timeExpired = (timeout != null && timeout);

        try {
            Result result = quizService.submitQuiz(state, user, timeExpired);
            model.addAttribute("result", result);
            model.addAttribute("unitName", quizService.getUnitById(state.getUnitId()).getName());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving results: " + e.getMessage());
        } finally {
            session.removeAttribute("quizState");
        }

        return "result";
    }
}