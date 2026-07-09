package com.example.lecture02.news;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NewsService {


    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public List<News> findAll() {

        return newsRepository.findAll();
    }

    public Optional<News> findNewsById(Long newsId) {
        return newsRepository.findById(newsId);
    }

    public News uploadNews(News news) {
        news.setAddedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }

    public News updateNewsById(Long newsId, News updatedNews) {

        News existingNews = newsRepository.findById(newsId).orElseThrow(() -> new RuntimeException("News not found!"));

        existingNews.setTitle(updatedNews.getTitle());
        existingNews.setContent(updatedNews.getContent());
        existingNews.setAddedBy(updatedNews.getAddedBy());

        existingNews.setAddedAt(LocalDateTime.now());

        return newsRepository.save(existingNews);
    }
}
