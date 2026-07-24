package com.example.lecture02.news.dto;

import java.time.LocalDateTime;

public record NewsResponse(
        Long newsId,
        String title,
        String content,
        String addedBy,
        LocalDateTime addedAt
) {
}