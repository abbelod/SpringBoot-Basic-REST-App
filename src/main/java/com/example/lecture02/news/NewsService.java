package com.example.lecture02.news;

import com.example.lecture02.news.dto.NewsResponse;
import com.example.lecture02.news.dto.UploadNewsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NewsService {


    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Cacheable(value="news")
    public Page<News> findAll(Pageable pageable) {
        return newsRepository.findAll(pageable);
    }

    public Page<News> searchNews(String keyWord, Pageable pageable) {
        return newsRepository.findByKeyword(keyWord, pageable);
    }

    public Optional<News> findNewsById(Long newsId) {
        return newsRepository.findById(newsId);
    }

    public NewsResponse uploadNews(UploadNewsRequest request, String username) {
        News news = new News();
        news.setContent(request.getContent());
        news.setTitle(request.getTitle());
        news.setAddedBy(username);
        news.setAddedAt(LocalDateTime.now());

        News savedNews = newsRepository.save(news);

        return new NewsResponse(
                savedNews.getNewsId(),
                savedNews.getTitle(),
                savedNews.getContent(),
                savedNews.getAddedBy(),
                savedNews.getAddedAt()
        );
    }

    public NewsResponse updateNewsById(Long newsId, UploadNewsRequest request, String username, boolean isEditor) throws AccessDeniedException {

        News existingNews = newsRepository.findById(newsId).orElseThrow(() -> new RuntimeException("News not found!"));

        if (!isEditor && !existingNews.getAddedBy().equals(username)) {
            throw new AccessDeniedException("Access Denied: Reporters can only edit their own news");
        }

        existingNews.setTitle(request.getTitle());
        existingNews.setContent(request.getContent());
        existingNews.setAddedAt(LocalDateTime.now());

        News savedNews = newsRepository.save(existingNews);
        return new NewsResponse(
                savedNews.getNewsId(),
                savedNews.getTitle(),
                savedNews.getContent(),
                savedNews.getAddedBy(),
                savedNews.getAddedAt()
        );
    }

    public boolean deleteNewsById(long newsId) {

        if (!newsRepository.existsById(newsId)) {
            return false;
        }
        newsRepository.deleteById(newsId);
        return true;
    }
}
