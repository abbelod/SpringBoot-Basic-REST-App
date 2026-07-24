package com.example.lecture02.news.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadNewsRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;
}
