package com.example.lecture02.news;


import com.example.lecture02.news.dto.NewsResponse;
import com.example.lecture02.news.dto.UploadNewsRequest;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<Page<NewsResponse>> findAll(@PageableDefault(size = 25) Pageable pageable) {
        Page<NewsResponse> responsePage = newsService.findAll(pageable)
                .map(news -> new NewsResponse(
                        news.getNewsId(),
                        news.getTitle(),
                        news.getContent(),
                        news.getAddedBy(),
                        news.getAddedAt()
                ));

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/id/{newsId}")
    public ResponseEntity<NewsResponse> findNewsById(@PathVariable Long newsId) {

        Optional<News> newsOpt = newsService.findNewsById(newsId);

        if (newsOpt.isPresent()) {
            News news = newsOpt.get();
            NewsResponse newsResponse = new NewsResponse(
                    news.getNewsId(),
                    news.getTitle(),
                    news.getContent(),
                    news.getAddedBy(),
                    news.getAddedAt());
            return ResponseEntity.ok(newsResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search/{keyWord}")
    public ResponseEntity<Page<NewsResponse>> searchNews(@PathVariable String keyWord, Pageable pageable) {
        Page<NewsResponse> responsePage = newsService.searchNews(keyWord, pageable)
                .map(news -> new NewsResponse(
                        news.getNewsId(),
                        news.getTitle(),
                        news.getContent(),
                        news.getAddedBy(),
                        news.getAddedAt()
                ));
        return ResponseEntity.ok(responsePage);
    }

    @PostMapping
    public ResponseEntity<NewsResponse> uploadNews(@RequestBody UploadNewsRequest request, Authentication authentication) {
        NewsResponse uploadedNews = newsService.uploadNews(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedNews);
    }

    @PostMapping("/id/{newsId}")
    public ResponseEntity<NewsResponse> updateNewsById(@PathVariable long newsId, @RequestBody UploadNewsRequest request, Authentication authentication) throws AccessDeniedException {

        boolean isEditor = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> Objects.equals(grantedAuthority.getAuthority(), "ROLE_EDITOR"));

        assert authentication != null;
        NewsResponse updatedNews = newsService.updateNewsById(newsId, request, authentication.getName(), isEditor);
        return ResponseEntity.ok(updatedNews);
    }

    @DeleteMapping("/id/{newsId}")
    public ResponseEntity<Void> deleteNewsById(@PathVariable long newsId) {
        boolean deleted = newsService.deleteNewsById(newsId);

        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }




}
