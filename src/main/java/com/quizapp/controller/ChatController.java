package com.quizapp.controller;

import com.quizapp.entity.*;
import com.quizapp.service.ChatbotService;
import com.quizapp.service.QuizService;
import com.quizapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private UserService userService;

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, String>>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<ChatMessage> list = chatbotService.getChatHistory(user);
        
        List<Map<String, String>> history = new ArrayList<>();
        for (ChatMessage msg : list) {
            history.add(Map.of(
                "sender", msg.getSender(),
                "text", msg.getMessageText(),
                "timestamp", msg.getTimestamp().toString()
            ));
        }
        return ResponseEntity.ok(history);
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestParam("message") String message,
                                                           @AuthenticationPrincipal UserDetails userDetails,
                                                           HttpSession session) {
        User user = userService.findByUsername(userDetails.getUsername());
        
        // Context awareness: retrieve active quiz state if present in the user's session
        QuizService.QuizSessionState activeQuizState = (QuizService.QuizSessionState) session.getAttribute("quizState");

        String tutorResponse = chatbotService.generateTutorResponse(user, message, activeQuizState);

        return ResponseEntity.ok(Map.of(
            "userMessage", message,
            "tutorResponse", tutorResponse
        ));
    }
}
