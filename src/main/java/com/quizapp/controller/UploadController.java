package com.quizapp.controller;

import com.quizapp.entity.*;
import com.quizapp.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;



import com.quizapp.repository.SubjectRepository;
import com.quizapp.repository.UnitRepository;


@Controller
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private UserService userService;


@Autowired
private UnitRepository unitRepository;

@Autowired
private SubjectRepository subjectRepository;

    @GetMapping
    public String uploadPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Upload> uploads = fileUploadService.getUserUploads(user);
        model.addAttribute("uploads", uploads);
        return "upload";
    }

    @PostMapping("/submit")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        User user = userService.findByUsername(userDetails.getUsername());

        if (file.isEmpty()) {
            return "redirect:/upload?error=File is empty. Please select a valid file.";
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = "TXT";
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".pdf")) fileType = "PDF";
            else if (lower.endsWith(".docx")) fileType = "DOCX";
            else if (lower.endsWith(".csv")) fileType = "CSV";
        }

        try {
            Upload upload = fileUploadService.saveUploadRecord(user, file, fileType);
            
            if ("CSV".equals(fileType)) {
                String subjectName = originalFilename.replaceAll("\\.[^.]+$", "");
                if (subjectName.length() > 0) {
                    subjectName = subjectName.substring(0, 1).toUpperCase() + subjectName.substring(1);
                } else {
                    subjectName = "CSV Import";
                }
                
                final String finalSubjectName = subjectName;
                Subject subject = subjectRepository.findFirstByNameIgnoreCase(finalSubjectName)
                        .orElseGet(() -> subjectRepository.save(Subject.builder()
                                .name(finalSubjectName)
                                .description(finalSubjectName + " curriculum from CSV")
                                .build()));
                
                List<Unit> existingUnits = unitRepository.findBySubject_Id(subject.getId().intValue());
                int nextLevel = existingUnits.stream().mapToInt(Unit::getLevelNumber).max().orElse(0) + 1;
                
                Unit unit = Unit.builder()
                        .name("Unit " + nextLevel + ": " + subjectName + " Import")
                        .description("CSV imported unit")
                        .subject(subject)
                        .levelNumber(nextLevel)
                        .build();
                unit = unitRepository.save(unit);
                
                int added = fileUploadService.parseAndSaveCsvQuestions(upload, file, unit.getId());
                return "redirect:/upload?success=" + added + " questions generated for Subject: " + subject.getName();
            } else {
                Map<String, Object> result = fileUploadService.processSubjectIndependentUpload(upload, file);
                String detectedSubject = (String) result.get("subjectName");
                int added = (Integer) result.get("questionsCount");
                return "redirect:/upload?success=" + added + " questions generated for Subject: " + detectedSubject;
            }
        } catch (Exception e) {
            return "redirect:/upload?error=Failed to process file: " + e.getMessage();
        }
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=quiz_questions_template.csv");

        try (PrintWriter writer = response.getWriter()) {
            // Write CSV Header
            writer.println("Question,Option A,Option B,Option C,Option D,Correct Option,Difficulty");
            // Write Sample Row 1
            writer.println("\"What keyword is used to define a constant in Java?\",\"const\",\"static\",\"final\",\"immutable\",\"C\",\"EASY\"");
            // Write Sample Row 2
            writer.println("\"Which collection class allows unique elements and retains insertion order?\",\"HashSet\",\"LinkedHashSet\",\"TreeSet\",\"ArrayList\",\"B\",\"MEDIUM\"");
        }
    }
}
