// src/main/java/com/quizapp/service/ChatbotService.java
package com.quizapp.service;

import com.quizapp.entity.*;
import com.quizapp.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatbotService {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private AiService aiService;

    public List<ChatMessage> getChatHistory(User user) {
        return chatHistoryRepository.findByUserOrderByTimestampAsc(user);
    }

    public ChatMessage saveMessage(User user, String sender, String text) {
        ChatMessage message = ChatMessage.builder()
                .user(user)
                .sender(sender)
                .messageText(text)
                .build();
        return chatHistoryRepository.save(message);
    }

    public String generateTutorResponse(User user, String userQuery, QuizService.QuizSessionState activeQuizState) {
        // Save user message
        saveMessage(user, "USER", userQuery);

        // Prepare context prompt
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an expert, encouraging personal CS tutor. Keep your explanations concise, structured, and easy to read. ");
        systemPrompt.append("Use markdown formatting. If the user asks for a hint, do not give the direct answer; guide them step-by-step instead. ");
        
        if (activeQuizState != null && activeQuizState.getCurrentQuestion() != null) {
            Question q = activeQuizState.getCurrentQuestion();
            systemPrompt.append(String.format("\n[Active Context] The user is currently on Level %d (Unit ID %d). They are viewing this question:\n", 
                    activeQuizState.getLevelNumber(), activeQuizState.getUnitId()));
            systemPrompt.append(String.format("Question: \"%s\"\n", q.getQuestionText()));
            systemPrompt.append(String.format("A) %s\nB) %s\nC) %s\nD) %s\n", q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));
            systemPrompt.append("Note: The correct option is " + q.getCorrectAnswer() + ". Help them learn the concept behind this question.");
        }

        String responseText = aiService.getChatResponse(user, systemPrompt.toString(), userQuery);
        if (responseText == null) {
            responseText = generateFallbackResponse(userQuery, activeQuizState);
        }

        // Save tutor response
        saveMessage(user, "TUTOR", responseText);

        return responseText;
    }

    private String generateFallbackResponse(String userQuery, QuizService.QuizSessionState activeQuizState) {
        String queryLower = userQuery.toLowerCase();
        
        if (activeQuizState != null && activeQuizState.getCurrentQuestion() != null) {
            Question q = activeQuizState.getCurrentQuestion();
            if (queryLower.contains("hint") || queryLower.contains("clue") || queryLower.contains("help")) {
                return "💡 **Tutor Hint:** Let's look at the options for the current question:\n" +
                        "1. Think about the definition of the concept: *\"" + q.getQuestionText() + "\"*\n" +
                        "2. Note that option " + q.getCorrectAnswer() + " represents the standard practice. \n" +
                        "Can you rule out any options that feel clearly incorrect?";
            }
            if (queryLower.contains("answer") || queryLower.contains("explain")) {
                return "🏫 **Tutor Explanation:** The correct option is **" + q.getCorrectAnswer() + "**. \n\n" +
                        "Here is the breakdown:\n" +
                        "*   The question asks: *" + q.getQuestionText() + "*\n" +
                        "*   Option A: `" + q.getOptionA() + "`\n" +
                        "*   Option B: `" + q.getOptionB() + "`\n" +
                        "*   Option C: `" + q.getOptionC() + "`\n" +
                        "*   Option D: `" + q.getOptionD() + "`\n\n" +
                        "Make sure to review this topic in your notes before moving to the next level!";
            }
        }

        if (queryLower.contains("hello") || queryLower.contains("hi") || queryLower.contains("hey")) {
            return "👋 **Hello! I'm your Chatbot Tutor.** \n\n" +
                    "I am here to help you study. You can ask me:\n" +
                    "*   *\"Give me a hint\"* (during an active quiz)\n" +
                    "*   *\"Explain the question\"* (to see the concept explanation)\n" +
                    "*   *\"Explain OOP\"* or *\"What is a variable?\"* for general concepts.";
        }
        
        if (queryLower.contains("oop") || queryLower.contains("object oriented") || queryLower.contains("polymorphism") || queryLower.contains("inheritance")) {
            return "☕ **OOP Concept Guide:**\n\n" +
                    "Object-Oriented Programming (OOP) is built on four core pillars:\n" +
                    "1.  **Encapsulation**: Hiding internal state via private variables and exposing public getters/setters.\n" +
                    "2.  **Inheritance**: Sharing behaviors/properties from superclass to subclass (using `extends`).\n" +
                    "3.  **Polymorphism**: The ability for an object to take many forms (e.g. Method Overriding or Overloading).\n" +
                    "4.  **Abstraction**: Hiding complex details and showing only essentials (using interface/abstract classes).";
        }

        if (queryLower.contains("variable") || queryLower.contains("syntax") || queryLower.contains("data types")) {
            return "💻 **Syntax & Variables Guide:**\n\n" +
                    "In Java, variables must declare a specific **Data Type**:\n" +
                    "*   `int` (integer: e.g., `42`)\n" +
                    "*   `double` (floating-point: e.g., `3.14`)\n" +
                    "*   `boolean` (logical state: `true` or `false`)\n" +
                    "*   `String` (object type representing characters, e.g., `\"Hello\"`)\n\n" +
                    "*Remember: Local variables inside methods do not get default values and must be initialized before use!*";
        }

        if (queryLower.contains("collection") || queryLower.contains("list") || queryLower.contains("map") || queryLower.contains("set")) {
            return "📚 **Java Collections Cheat Sheet:**\n\n" +
                    "*   **List** (Ordered, permits duplicates): e.g., `ArrayList`, `LinkedList`.\n" +
                    "*   **Set** (Unordered, denies duplicates): e.g., `HashSet`, `TreeSet`.\n" +
                    "*   **Map** (Key-Value associations, unique keys): e.g., `HashMap`, `TreeMap`.\n\n" +
                    "For example, use a `HashMap` when you need O(1) key lookups!";
        }

        return "🤖 **Tutor Response:** I'm glad you asked about that! \n\n" +
                "To ensure you are ready for the exam:\n" +
                "1. Study the core definitions of this unit.\n" +
                "2. Try writing a quick test class to see it in code compile.\n" +
                "Would you like me to quiz you on a random topic from your current level?";
    }
}