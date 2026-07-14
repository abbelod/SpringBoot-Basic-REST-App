package com.example.lecture02.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NewsService {


    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public Page<News> findAll(Pageable pageable) {
        return newsRepository.findAll(pageable);
    }

    public Optional<News> findNewsById(Long newsId) {
        return newsRepository.findById(newsId);
    }

    public News uploadNews(News news, String username) {
        news.setAddedBy(username);
        news.setAddedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }

    public News updateNewsById(Long newsId, News updatedNews, String username, boolean isEditor) throws AccessDeniedException {

        News existingNews = newsRepository.findById(newsId).orElseThrow(() -> new RuntimeException("News not found!"));

        if(!isEditor && !existingNews.getAddedBy().equals(username)) {
            throw new AccessDeniedException("Access Denied: Reporters can only edit their own news");
        }

        existingNews.setTitle(updatedNews.getTitle());
        existingNews.setContent(updatedNews.getContent());
        existingNews.setAddedAt(LocalDateTime.now());

        return newsRepository.save(existingNews);
    }

    public boolean deleteNewsById(long newsId) {

        if(!newsRepository.existsById(newsId)) {
            return false;
        }
        newsRepository.deleteById(newsId);
        return true;
    }
}
