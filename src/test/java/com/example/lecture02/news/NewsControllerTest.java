package com.example.lecture02.news;

import com.example.lecture02.news.dto.NewsResponse;
import com.example.lecture02.news.dto.UploadNewsRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NewsController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
public class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnNewsById() throws Exception {

        News news = new News();
        news.setNewsId(1L);
        news.setTitle("Test Title");
        news.setContent("Test Content");

        Mockito.when(newsService.findNewsById(1L)).thenReturn(Optional.of(news));

        mockMvc.perform(get("/api/v1/news/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(1))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));

    }

    @Test
    void shouldReturn404WhenNewsDoesNotExist() throws Exception {

        Mockito.when(newsService.findNewsById(100L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/news/id/100"))
                .andExpect(status().isNotFound());

    }

    @Test
    void shouldReturnAllNews() throws Exception {

        News news1 = new News();
        news1.setNewsId(1L);
        news1.setTitle("News 1 title test");
        news1.setContent("News 1 content test");

        News news2 = new News();
        news2.setNewsId(2L);
        news2.setTitle("News 2 title test");
        news2.setContent("News 2 content test");

        Page<News> page = new PageImpl<>(List.of(news1, news2));

        Mockito.when(newsService.findAll(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsId").value(1))
                .andExpect(jsonPath("$.content[0].title").value("News 1 title test"))
                .andExpect(jsonPath("$.content[1].newsId").value(2))
                .andExpect(jsonPath("$.content[1].title").value("News 2 title test"));
    }

}
