package com.example.lecture02.news;


import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.plaf.OptionPaneUI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService){
        this.newsService = newsService;
    }

    @GetMapping
    public Page<News> findAll(@PageableDefault(size = 25) Pageable pageable) {
        return newsService.findAll(pageable);
    }


    @GetMapping("/id/{newsId}")
    public ResponseEntity<News> findNewsById(@PathVariable Long newsId) {
        Optional<News> news =  newsService.findNewsById(newsId);

        if(news.isPresent()) {
            return ResponseEntity.ok(news.get());
        }
        return ResponseEntity.notFound().build();
    }


    @PostMapping
    public ResponseEntity<News> uploadNews(@RequestBody News news) {
        News uploadedNews = newsService.uploadNews(news);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedNews);
    }

    @PostMapping("/id/{newsId}")
    public ResponseEntity<News> updateNewsById(@PathVariable long newsId, @RequestBody News news) {
        News updatedNews = newsService.updateNewsById(newsId, news);
        return ResponseEntity.ok(updatedNews);
    }

    @DeleteMapping("/id/{newsId}")
    public ResponseEntity<Void> deleteNewsById(@PathVariable long newsId) {
        boolean deleted = newsService.deleteNewsById(newsId);

        if(deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


}
