// src/main/java/com/quizapp/service/AiService.java
package com.quizapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    @Value("${app.openai.api-key:}")
    private String globalOpenAiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String globalOpenAiModel;

    @Value("${app.gemini.api-key:}")
    private String globalGeminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getChatResponse(User user, String systemPrompt, String userQuery) {
        String provider = resolveProvider(user);
        String apiKey = resolveApiKey(user);

        if ("OFFLINE".equalsIgnoreCase(provider) || apiKey == null || apiKey.trim().isEmpty()) {
            return null; // Return null to indicate fallback to local simulator
        }

        if ("GEMINI".equalsIgnoreCase(provider)) {
            return callGeminiChat(apiKey, systemPrompt, userQuery);
        } else {
            return callOpenAiChat(apiKey, systemPrompt, userQuery);
        }
    }

    public List<Question> generateQuestions(User user, String textContent, Unit unit, Upload upload) {
        String provider = resolveProvider(user);
        String apiKey = resolveApiKey(user);

        if ("OFFLINE".equalsIgnoreCase(provider) || apiKey == null || apiKey.trim().isEmpty()) {
            return Collections.emptyList(); // Let FileUploadService handle fallback
        }

        String prompt = "You are an automated MCQ question generator. Generate exactly 5 multiple-choice questions based on the provided study material. " +
                "The response must be a valid JSON array of objects. Do not wrap it in markdown code blocks. Each object must have these exact keys:\n" +
                "{\n" +
                "  \"questionText\": \"question content\",\n" +
                "  \"optionA\": \"first option\",\n" +
                "  \"optionB\": \"second option\",\n" +
                "  \"optionC\": \"third option\",\n" +
                "  \"optionD\": \"fourth option\",\n" +
                "  \"correctAnswer\": \"A/B/C/D\",\n" +
                "  \"difficulty\": \"EASY/MEDIUM/HARD\"\n" +
                "}\n\n" +
                "Study material:\n" + textContent.substring(0, Math.min(textContent.length(), 6000));

        String jsonResponse = "";
        try {
            if ("GEMINI".equalsIgnoreCase(provider)) {
                jsonResponse = callGeminiGenerate(apiKey, prompt, true);
            } else {
                jsonResponse = callOpenAiGenerate(apiKey, prompt, true);
            }

            // Robust JSON extraction
            jsonResponse = extractJsonArray(jsonResponse);

            List<Map<String, String>> parsedList = objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {});
            List<Question> questionsList = new ArrayList<>();

            for (Map<String, String> qMap : parsedList) {
                String correctAns = qMap.getOrDefault("correctAnswer", "A").trim().toUpperCase();
                if (!Arrays.asList("A", "B", "C", "D").contains(correctAns)) {
                    correctAns = "A";
                }
                String difficulty = qMap.getOrDefault("difficulty", "MEDIUM").trim().toUpperCase();
                if (!Arrays.asList("EASY", "MEDIUM", "HARD").contains(difficulty)) {
                    difficulty = "MEDIUM";
                }

                Question q = Question.builder()
                        .unit(unit)
                        .upload(upload)
                        .questionText(qMap.get("questionText"))
                        .optionA(qMap.get("optionA"))
                        .optionB(qMap.get("optionB"))
                        .optionC(qMap.get("optionC"))
                        .optionD(qMap.get("optionD"))
                        .correctAnswer(correctAns)
                        .difficulty(difficulty)
                        .isInbuilt(false)
                        .build();
                questionsList.add(q);
            }
            return questionsList;
        } catch (Exception e) {
            System.err.println("AI question generation failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> parseStudyMaterial(User user, String textContent) {
        String provider = resolveProvider(user);
        String apiKey = resolveApiKey(user);

        if ("OFFLINE".equalsIgnoreCase(provider) || apiKey == null || apiKey.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String prompt = "You are an AI assistant that analyzes study material and automatically generates a structured course curriculum with multiple-choice questions.\n" +
                "Read the following study material and identify the core subject name (e.g., 'Python', 'DBMS', 'Data Structures', 'Web Development').\n" +
                "Then, divide the material into 1 to 3 distinct logical learning units (topics).\n" +
                "For each unit, generate exactly 5 multiple-choice questions of mixed difficulty (EASY, MEDIUM, HARD) based on that unit's content.\n" +
                "Even if the study material is short, ambiguous, or lacks detail, you must still generate a valid JSON object with a best-guess subject name and at least 3 questions based on whatever context is available.\n" +
                "The response must be a single, valid JSON object with the following structure, with no markdown formatting or backticks:\n" +
                "{\n" +
                "  \"subjectName\": \"Subject Title\",\n" +
                "  \"subjectDescription\": \"Brief description of the subject\",\n" +
                "  \"units\": [\n" +
                "    {\n" +
                "      \"unitName\": \"Unit 1: Topic Name\",\n" +
                "      \"unitDescription\": \"Description of what this unit covers\",\n" +
                "      \"questions\": [\n" +
                "        {\n" +
                "          \"questionText\": \"MCQ question text\",\n" +
                "          \"optionA\": \"Option A content\",\n" +
                "          \"optionB\": \"Option B content\",\n" +
                "          \"optionC\": \"Option C content\",\n" +
                "          \"optionD\": \"Option D content\",\n" +
                "          \"correctAnswer\": \"A/B/C/D\",\n" +
                "          \"difficulty\": \"EASY/MEDIUM/HARD\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Study material content:\n" + textContent.substring(0, Math.min(textContent.length(), 6000));

        String jsonResponse = "";
        try {
            if ("GEMINI".equalsIgnoreCase(provider)) {
                jsonResponse = callGeminiGenerate(apiKey, prompt, true);
            } else {
                jsonResponse = callOpenAiGenerate(apiKey, prompt, true);
            }

            jsonResponse = extractJsonObject(jsonResponse);
            return objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            System.err.println("AI study material parsing failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String resolveProvider(User user) {
        if (user != null && user.getApiProvider() != null && !user.getApiProvider().trim().isEmpty()) {
            return user.getApiProvider().toUpperCase();
        }
        if (globalGeminiApiKey != null && !globalGeminiApiKey.trim().isEmpty() && !"none".equalsIgnoreCase(globalGeminiApiKey)) {
            return "GEMINI";
        }
        if (globalOpenAiKey != null && !globalOpenAiKey.trim().isEmpty() && !"none".equalsIgnoreCase(globalOpenAiKey)) {
            return "OPENAI";
        }
        return "OFFLINE";
    }

    private String resolveApiKey(User user) {
        if (user != null && user.getApiKey() != null && !user.getApiKey().trim().isEmpty()) {
            return user.getApiKey();
        }
        String provider = resolveProvider(user);
        if ("GEMINI".equalsIgnoreCase(provider)) {
            return (globalGeminiApiKey != null && !"none".equalsIgnoreCase(globalGeminiApiKey)) ? globalGeminiApiKey : "";
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            return (globalOpenAiKey != null && !"none".equalsIgnoreCase(globalOpenAiKey)) ? globalOpenAiKey : "";
        }
        return "";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String callOpenAiChat(String apiKey, String systemPrompt, String userQuery) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", globalOpenAiModel);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userQuery)
            ));
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return "⚠️ OpenAI returned an empty chat response.";
        } catch (Exception e) {
            return "⚠️ Error calling OpenAI API: " + e.getMessage();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String callOpenAiGenerate(String apiKey, String prompt, boolean isJson) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", globalOpenAiModel);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.3);
            if (isJson) {
                requestBody.put("response_format", Map.of("type", "json_object"));
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return "";
        } catch (Exception e) {
            System.err.println("OpenAI API invocation failed: " + e.getMessage());
            return "";
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String callGeminiChat(String apiKey, String systemPrompt, String userQuery) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", systemPrompt)));
            requestBody.put("systemInstruction", systemInstruction);

            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", List.of(Map.of("text", userQuery)));
            requestBody.put("contents", List.of(contentMap));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    if (content != null) {
                        List parts = (List) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map part = (Map) parts.get(0);
                            return (String) part.get("text");
                        }
                    }
                }
            }
            return "⚠️ Gemini returned an empty chat response.";
        } catch (Exception e) {
            return "⚠️ Error calling Gemini API: " + e.getMessage();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String callGeminiGenerate(String apiKey, String prompt, boolean isJson) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", List.of(Map.of("text", prompt)));
            requestBody.put("contents", List.of(contentMap));

            if (isJson) {
                requestBody.put("generationConfig", Map.of("responseMimeType", "application/json"));
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    if (content != null) {
                        List parts = (List) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map part = (Map) parts.get(0);
                            return (String) part.get("text");
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            System.err.println("Gemini API invocation failed: " + e.getMessage());
            return "";
        }
    }

    private String extractJsonArray(String content) {
        if (content == null) return "[]";
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start != -1 && end != -1 && start < end) {
            return content.substring(start, end + 1);
        }
        return content.trim();
    }

    private String extractJsonObject(String content) {
        if (content == null) return "{}";
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start != -1 && end != -1 && start < end) {
            return content.substring(start, end + 1);
        }
        return content.trim();
    }
}