package com.example.lecture02.news;


import org.springframework.web.bind.annotation.*;

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
    public List<News> findAll() {
        return newsService.findAll();
    }


    @GetMapping("/id/{newsId}")
    public Optional<News> findNewsById(@PathVariable Long newsId) {
        return newsService.findNewsById(newsId);
    }


    @PostMapping
    public News uploadNews(@RequestBody News news) {
        return newsService.uploadNews(news);
    }

    @PostMapping("/id/{newsId}")
    public News updateNewsById(@PathVariable long newsId, @RequestBody News news) {
        return newsService.updateNewsById(newsId, news);
    }


}
