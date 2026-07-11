// src/main/java/com/quizapp/service/FileUploadService.java
package com.quizapp.service;

import com.quizapp.entity.*;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.UploadRepository;
import com.quizapp.repository.SubjectRepository;
import com.quizapp.repository.UnitRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FileUploadService {

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizService quizService;

    @Autowired
    private AiService aiService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UnitRepository unitRepository;

    public List<Upload> getUserUploads(User user) {
        return uploadRepository.findByUserOrderByUploadTimeDesc(user);
    }

    @Transactional
    public Upload saveUploadRecord(User user, MultipartFile file, String type) {
        Upload upload = Upload.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .fileType(type)
                .fileSize(file.getSize())
                .build();
        return uploadRepository.save(upload);
    }

    @Transactional
    public int parseAndSaveCsvQuestions(Upload upload, MultipartFile file, Integer unitId) throws Exception {
        Unit unit = quizService.getUnitById(unitId);
        int questionsAdded = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                
                if (isHeader) {
                    isHeader = false;
                    if (columns[0].toLowerCase().contains("question") || columns[0].toLowerCase().contains("text")) {
                        continue;
                    }
                }

                if (columns.length >= 7) {
                    String questionText = cleanCsvField(columns[0]);
                    String optA = cleanCsvField(columns[1]);
                    String optB = cleanCsvField(columns[2]);
                    String optC = cleanCsvField(columns[3]);
                    String optD = cleanCsvField(columns[4]);
                    String correctAns = cleanCsvField(columns[5]).trim().toUpperCase();
                    String difficulty = cleanCsvField(columns[6]).trim().toUpperCase();

                    if (!Arrays.asList("A", "B", "C", "D").contains(correctAns)) {
                        correctAns = "A";
                    }
                    if (!Arrays.asList("EASY", "MEDIUM", "HARD").contains(difficulty)) {
                        difficulty = "MEDIUM";
                    }

                    Question question = Question.builder()
                            .unit(unit)
                            .upload(upload)
                            .questionText(questionText)
                            .optionA(optA)
                            .optionB(optB)
                            .optionC(optC)
                            .optionD(optD)
                            .correctAnswer(correctAns)
                            .difficulty(difficulty)
                            .isInbuilt(false)
                            .build();

                    questionRepository.save(question);
                    questionsAdded++;
                }
            }
        }
        return questionsAdded;
    }

    private String cleanCsvField(String field) {
        if (field == null) return "";
        field = field.trim();
        if (field.startsWith("\"") && field.endsWith("\"")) {
            field = field.substring(1, field.length() - 1);
        }
        return field.replace("\"\"", "\"");
    }

    @Transactional
    public int generateAiQuestions(Upload upload, MultipartFile file, Integer unitId) throws Exception {
        Unit unit = quizService.getUnitById(unitId);
        
        String textContent = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                textContent = stripper.getText(document);
            }
        } else {
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
            }
            textContent = contentBuilder.toString();
        }
        
        List<Question> generatedQuestions = aiService.generateQuestions(upload.getUser(), textContent, unit, upload);
        if (generatedQuestions.isEmpty()) {
            generatedQuestions = generateFallbackAiQuestions(textContent, unit, upload);
        }

        if (!generatedQuestions.isEmpty()) {
            questionRepository.saveAll(generatedQuestions);
        }
        return generatedQuestions.size();
    }

    private List<Question> generateFallbackAiQuestions(String text, Unit unit, Upload upload) {
        List<Question> questions = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return getHardcodedFallbackQuestions(unit, upload);
        }

        String cleanText = text.replaceAll("\\s+", " ");
        String[] sentences = cleanText.split("(?<=[\\.?!])\\s+");

        List<String> validSentences = new ArrayList<>();
        List<String> vocabList = new ArrayList<>();

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 20 && sentence.length() < 300 && !sentence.contains("http") && !sentence.contains("%")) {
                validSentences.add(sentence);
            }
            String[] words = sentence.split("[^a-zA-Z]+");
            for (String w : words) {
                if (w.length() > 3 && w.length() < 15 && Character.isLowerCase(w.charAt(0))) {
                    String lower = w.toLowerCase();
                    if (!vocabList.contains(lower)) {
                        vocabList.add(lower);
                    }
                }
            }
        }

        Collections.shuffle(validSentences);
        Collections.shuffle(vocabList);

        int count = 0;
        for (String sentence : validSentences) {
            if (count >= 5) break;
            
            String[] words = sentence.split("[^a-zA-Z]+");
            String targetWord = null;
            for (String w : words) {
                if (w.length() > 3 && vocabList.contains(w.toLowerCase())) {
                    targetWord = w;
                    break;
                }
            }

            if (targetWord != null) {
                String questionText = "According to the uploaded study material, fill in the blank: \"" + sentence.replace(targetWord, "_______") + "\"";
                
                List<String> options = new ArrayList<>();
                options.add(targetWord);
                for (String dist : vocabList) {
                    if (options.size() >= 4) break;
                    if (!dist.equalsIgnoreCase(targetWord) && dist.length() > 3) {
                        options.add(dist);
                    }
                }
                
                if (options.size() < 4) {
                    String[] fallbacks = {"variable", "function", "compile", "runtime", "memory", "inheritance"};
                    for (String fb : fallbacks) {
                        if (options.size() >= 4) break;
                        if (!options.contains(fb)) {
                            options.add(fb);
                        }
                    }
                }

                Collections.shuffle(options);
                
                String optA = options.get(0);
                String optB = options.get(1);
                String optC = options.get(2);
                String optD = options.get(3);
                
                String correctAns = "A";
                if (optA.equalsIgnoreCase(targetWord)) correctAns = "A";
                else if (optB.equalsIgnoreCase(targetWord)) correctAns = "B";
                else if (optC.equalsIgnoreCase(targetWord)) correctAns = "C";
                else if (optD.equalsIgnoreCase(targetWord)) correctAns = "D";

                questions.add(Question.builder()
                        .unit(unit)
                        .upload(upload)
                        .isInbuilt(false)
                        .difficulty("MEDIUM")
                        .questionText(questionText)
                        .optionA(optA)
                        .optionB(optB)
                        .optionC(optC)
                        .optionD(optD)
                        .correctAnswer(correctAns)
                        .build());
                count++;
            }
        }
        // Ensure we always return at least 5 questions for a complete quiz
        if (questions.size() < 5) {
            List<Question> templates = getHardcodedFallbackQuestions(unit, upload);
            for (Question q : templates) {
                if (questions.size() >= 5) break;
                boolean alreadyContains = questions.stream()
                        .anyMatch(existing -> existing.getQuestionText().equalsIgnoreCase(q.getQuestionText()));
                if (!alreadyContains) {
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    private List<Question> getHardcodedFallbackQuestions(Unit unit, Upload upload) {
        String subjectName = (unit.getSubject() != null) ? unit.getSubject().getName() : "Study Material";
        List<Question> questions = new ArrayList<>();
        questions.add(Question.builder()
                .unit(unit).upload(upload).isInbuilt(false).difficulty("EASY")
                .questionText("Which of the following best describes the core concept of " + subjectName + "?")
                .optionA("It is a structured framework for managing data and logic").optionB("It is an offline compiler tool").optionC("It is a hardware storage module").optionD("It is an operating system protocol")
                .correctAnswer("A").build());
        questions.add(Question.builder()
                .unit(unit).upload(upload).isInbuilt(false).difficulty("MEDIUM")
                .questionText("What is a primary advantage of utilizing " + subjectName + " in software engineering?")
                .optionA("Decreased execution speed").optionB("Enhanced modularity, scalability, and code reusability").optionC("Elimination of all runtime bugs").optionD("Reduced memory usage to absolute zero")
                .correctAnswer("B").build());
        questions.add(Question.builder()
                .unit(unit).upload(upload).isInbuilt(false).difficulty("HARD")
                .questionText("Which architectural pattern or programming standard is most commonly associated with modern implementations of " + subjectName + "?")
                .optionA("Procedural linear design").optionB("Object-oriented or structured design paradigms").optionC("Assembly level instructions").optionD("Direct register allocations")
                .correctAnswer("B").build());
        return questions;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> processSubjectIndependentUpload(Upload upload, MultipartFile file) throws Exception {
        String textContent = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                textContent = stripper.getText(document);
            }
        } else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                try (XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    textContent = extractor.getText();
                }
            }
        } else {
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
            }
            textContent = contentBuilder.toString();
        }

        User user = upload.getUser();
        Map<String, Object> aiResult = aiService.parseStudyMaterial(user, textContent);

        String subjectName;
        String subjectDescription = "Auto-generated from uploaded study material";
        List<Map<String, Object>> aiUnits = new ArrayList<>();

        if (aiResult != null && !aiResult.isEmpty() && aiResult.containsKey("subjectName")) {
            subjectName = (String) aiResult.get("subjectName");
            if (aiResult.containsKey("subjectDescription")) {
                subjectDescription = (String) aiResult.get("subjectDescription");
            }
            if (aiResult.containsKey("units")) {
                aiUnits = (List<Map<String, Object>>) aiResult.get("units");
            }
        } else {
            subjectName = detectSubjectFromText(textContent, originalFilename);
        }

        // Clean subject name
        if (subjectName == null || subjectName.trim().isEmpty()) {
            subjectName = "Study Material";
        }
        subjectName = subjectName.trim();

        final String finalSubjectName = subjectName;
        final String finalSubjectDesc = subjectDescription;
        Subject subject = subjectRepository.findFirstByNameIgnoreCase(finalSubjectName)
                .orElseGet(() -> {
                    Subject sub = Subject.builder()
                            .name(finalSubjectName)
                            .description(finalSubjectDesc)
                            .build();
                    return subjectRepository.save(sub);
                });

        // Find current max level number for this subject to append units sequentially
        List<Unit> existingUnits = unitRepository.findBySubject_Id(subject.getId().intValue());
        int currentMaxLevel = existingUnits.stream()
                .mapToInt(Unit::getLevelNumber)
                .max()
                .orElse(0);

        int questionsAdded = 0;

        if (!aiUnits.isEmpty()) {
            for (Map<String, Object> uMap : aiUnits) {
                currentMaxLevel++;
                String uName = (String) uMap.getOrDefault("unitName", "Unit " + currentMaxLevel);
                String uDesc = (String) uMap.getOrDefault("unitDescription", "No description provided");

                Unit unit = Unit.builder()
                        .name(uName)
                        .description(uDesc)
                        .subject(subject)
                        .levelNumber(currentMaxLevel)
                        .build();
                unit = unitRepository.save(unit);

                List<Map<String, Object>> aiQuestions = (List<Map<String, Object>>) uMap.get("questions");
                int unitQuestionsCount = 0;
                if (aiQuestions != null) {
                    for (Map<String, Object> qMap : aiQuestions) {
                        String correctAns = qMap.containsKey("correctAnswer") ? String.valueOf(qMap.get("correctAnswer")).trim().toUpperCase() : "A";
                        if (!Arrays.asList("A", "B", "C", "D").contains(correctAns)) {
                            correctAns = "A";
                        }
                        String difficulty = qMap.containsKey("difficulty") ? String.valueOf(qMap.get("difficulty")).trim().toUpperCase() : "MEDIUM";
                        if (!Arrays.asList("EASY", "MEDIUM", "HARD").contains(difficulty)) {
                            difficulty = "MEDIUM";
                        }

                        Question q = Question.builder()
                                .unit(unit)
                                .upload(upload)
                                .questionText(String.valueOf(qMap.get("questionText")))
                                .optionA(String.valueOf(qMap.get("optionA")))
                                .optionB(String.valueOf(qMap.get("optionB")))
                                .optionC(String.valueOf(qMap.get("optionC")))
                                .optionD(String.valueOf(qMap.get("optionD")))
                                .correctAnswer(correctAns)
                                .difficulty(difficulty)
                                .isInbuilt(false)
                                .build();
                        questionRepository.save(q);
                        questionsAdded++;
                        unitQuestionsCount++;
                    }
                }
                if (unitQuestionsCount == 0) {
                    List<Question> fallbackQuestions = generateFallbackAiQuestions(textContent, unit, upload);
                    for (Question q : fallbackQuestions) {
                        questionRepository.save(q);
                        questionsAdded++;
                    }
                }
            }
        } else {
            currentMaxLevel++;
            String cleanFilename = originalFilename != null ? originalFilename.replaceAll("\\.[^.]+$", "") : "Study Material";
            String uName = "Unit " + currentMaxLevel + ": " + cleanFilename;
            Unit unit = Unit.builder()
                    .name(uName)
                    .description("Auto-generated study unit")
                    .subject(subject)
                    .levelNumber(currentMaxLevel)
                    .build();
            unit = unitRepository.save(unit);

            List<Question> fallbackQuestions = generateFallbackAiQuestions(textContent, unit, upload);
            for (Question q : fallbackQuestions) {
                questionRepository.save(q);
                questionsAdded++;
            }
        }

        return Map.of(
            "subjectName", subject.getName(),
            "questionsCount", questionsAdded
        );
    }

    private String detectSubjectFromText(String text, String filename) {
        if (text == null) text = "";
        String lower = text.toLowerCase();
        if (lower.contains("python")) return "Python";
        if (lower.contains("java ") || lower.contains("java\n") || lower.contains("jdk")) return "Java";
        if (lower.contains("database") || lower.contains("dbms") || lower.contains("sql") || lower.contains("mongodb")) return "DBMS";
        if (lower.contains("data structure") || lower.contains("algorithm") || lower.contains("array") || lower.contains("tree") || lower.contains("linked list")) return "Data Structures";
        if (lower.contains("javascript") || lower.contains("html") || lower.contains("css") || lower.contains("web dev") || lower.contains("react")) return "Web Development";
        
        if (filename != null && !filename.isEmpty()) {
            String name = filename.replaceAll("\\.[^.]+$", "");
            if (name.length() > 0) {
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
        }
        return "Study Material";
    }
}