package com.example.lecture02.news;

import org.apache.commons.collections4.functors.ExceptionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class NewsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {

        newsRepository.deleteAll();

        News news1 = new News();
        news1.setNewsId(1L);
        news1.setTitle("Spring Boot");
        news1.setContent("Spring Boot Content");

        News news2 = new News();
        news2.setNewsId(2L);
        news2.setTitle("Java");
        news2.setContent("Java Content");

        newsRepository.save(news1);
        newsRepository.save(news2);

    }

    @Test
    void shouldReturnAllNews() throws Exception {

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot"))
                .andExpect(jsonPath("$.content[1].title").value("Java"));

    }

    @Test
    void shouldReturnNewsById() throws Exception {

        mockMvc.perform(get("/api/v1/news/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(1))
                .andExpect(jsonPath("$.title").value("Spring Boot"));
    }

    @Test
    void shouldCreateNews() throws Exception {

        News news = new News();
        news.setNewsId(10L);
        news.setTitle("Spring Boot");
        news.setContent("Learning Spring Boot");


        mockMvc.perform(post("/api/v1/news")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(news)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsId").exists())
                .andExpect(jsonPath("$.title").value("Spring Boot"))
                .andExpect(jsonPath("$.content").value("Learning Spring Boot"));

        List<News> allNews = newsRepository.findAll();

        assertEquals(3, allNews.size());
    }

    @Test
    void shouldUpdateNews() throws Exception {

        News news = new News();
        news.setNewsId(1L);
        news.setTitle("Updated Title");
        news.setContent("Updated Content");

        mockMvc.perform(get("/api/v1/news/id/1"))
                .andExpect(jsonPath("$.content").value("Spring Boot Content"))
                .andExpect(jsonPath("$.title").value("Spring Boot"));

        mockMvc.perform(post("/api/v1/news/id/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(news)))
                        .andExpect(jsonPath("$.content").value("Updated Content"))
                        .andExpect(jsonPath("$.title").value("Updated Title"));

        Optional<News> newsInDb = newsRepository.findById(1L);
        assertEquals(newsInDb.isPresent(), true);
        assertEquals(newsInDb.get().getTitle(), "Updated Title");
        assertEquals(newsInDb.get().getContent(), "Updated Content");

    }

    @Test
    void shouldDeleteNews() throws Exception {

        mockMvc.perform(get("/api/v1/news/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Spring Boot Content"))
                .andExpect(jsonPath("$.title").value("Spring Boot"));

        mockMvc.perform(delete("/api/v1/news/id/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/news/id/1"))
                .andExpect(status().isNotFound());

    }

}
