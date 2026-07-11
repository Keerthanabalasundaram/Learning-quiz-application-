// src/main/java/com/quizapp/controller/SettingsController.java
package com.quizapp.controller;

import com.quizapp.entity.User;
import com.quizapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String settingsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "settings";
    }

    @GetMapping("/get")
    @ResponseBody
    public Map<String, String> getSettings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        String maskedKey = "";
        if (user.getApiKey() != null && !user.getApiKey().isEmpty()) {
            maskedKey = "******";
        }
        return Map.of(
            "apiProvider", user.getApiProvider() != null ? user.getApiProvider() : "OFFLINE",
            "apiKey", maskedKey
        );
    }

    @PostMapping("/save")
    public String saveSettings(@RequestParam("apiProvider") String apiProvider,
                               @RequestParam(value = "apiKey", required = false) String apiKey,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpServletRequest request) {
        User user = userService.findByUsername(userDetails.getUsername());
        
        String resolvedKey = (apiKey != null) ? apiKey.trim() : "";
        
        if ("******".equals(resolvedKey) || (resolvedKey.isEmpty() && !"OFFLINE".equalsIgnoreCase(apiProvider))) {
            resolvedKey = user.getApiKey();
        }

        userService.updateUserSettings(user, resolvedKey, apiProvider.toUpperCase());

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            if (referer.contains("?")) {
                referer = referer.substring(0, referer.indexOf("?"));
            }
            return "redirect:" + referer + "?settingsSaved=true";
        }
        return "redirect:/dashboard?settingsSaved=true";
    }
}