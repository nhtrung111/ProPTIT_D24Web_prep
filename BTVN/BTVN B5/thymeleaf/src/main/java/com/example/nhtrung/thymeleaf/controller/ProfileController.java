package com.example.nhtrung.thymeleaf.controller;

import com.example.nhtrung.thymeleaf.model.PersonalInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String showProfile(Model model) {
        PersonalInfo info = PersonalInfo.builder()
                .fullName("Nguyen Hoang Trung")
                .title("Random homecook")
                .bio("Passionate about Italian forgotten classics.")
                .email("nhtrung2906@gmail.com")
                .instagram("https://www.instagram.com/_nhtrungg__/")
                .achievement(List.of("\"Author, 'Precision & Heat' – Winner of the 2025 Asian Culinary Arts Digital Book Award\""))
                .experience(List.of("\"3+ Years Line Cook – High-volume Italian & Asian fusion kitchens (Sauté, Grill, & Prep stations)\", \"Kitchen Operations & Inventory – Menu costing, food safety standards (HACCP), and prep schedule optimization\""))
                .hobbies(List.of(
                        "Football Tactics & Analytics",
                        "Perfumery & Gourmand Fragrance Notes",
                        "Music Addict"
                ))
                .build();

        model.addAttribute("profile", info);
        return "profile";
    }
}