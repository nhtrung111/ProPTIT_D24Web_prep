package com.example.nhtrung.thymeleaf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalInfo {
    private String fullName;
    private String title;
    private String bio;
    private String email;
    private String instagram;
    private List<String> achievement;
    private List<String> experience;
    private List<String> hobbies;
}