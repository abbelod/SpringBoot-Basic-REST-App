package com.example.lecture02.news;

import com.example.lecture02.config.DatabaseSeeder;
import com.example.lecture02.config.SecurityConfig;
import com.example.lecture02.user.User;
import com.example.lecture02.user.UserRepository;
import org.apache.commons.collections4.functors.ExceptionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class NewsControllerIntegrationTest {

    Long newsId;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NewsService newsService;

    @BeforeEach
    void setup() {

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        newsRepository.deleteAll();

        News news1 = new News();
        news1.setTitle("Spring Boot");
        news1.setContent("Spring Boot Content");


        News news2 = new News();
        news2.setTitle("Java");
        news2.setContent("Java Content");

        News savedNews1 = newsRepository.save(news1);
        this.newsId = savedNews1.getNewsId();

        newsRepository.save(news2);

    }

    @Test
    @WithMockUser(username = "readerUser", roles = {"READER"})
    void shouldReturnAllNews() throws Exception {

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot"))
                .andExpect(jsonPath("$.content[1].title").value("Java"));

    }

    @Test
    @WithMockUser(username = "readerUser", roles = {"READER"})
    void shouldReturnNewsById() throws Exception {

        mockMvc.perform(get("/api/v1/news/id/" + this.newsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(this.newsId))
                .andExpect(jsonPath("$.title").value("Spring Boot"));
    }

    @Test
    @WithMockUser(username = "reporterUser", roles = {"REPORTER"})
    void shouldCreateNews() throws Exception {

        News news = new News();
        news.setNewsId(10L);
        news.setTitle("Spring Boot");
        news.setContent("Learning Spring Boot");


        mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(news))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsId").exists())
                .andExpect(jsonPath("$.title").value("Spring Boot"))
                .andExpect(jsonPath("$.addedBy").value("reporterUser"))
                .andExpect(jsonPath("$.content").value("Learning Spring Boot"));

        List<News> allNews = newsRepository.findAll();

        assertEquals(3, allNews.size());
    }

    @Test
    @WithMockUser(username = "editorUser", roles = {"EDITOR"})
    void shouldDeleteNews() throws Exception {

        mockMvc.perform(get("/api/v1/news/id/" + newsId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Spring Boot Content"))
                .andExpect(jsonPath("$.title").value("Spring Boot"));

        mockMvc.perform(delete("/api/v1/news/id/" + newsId).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/news/id/" + newsId).with(csrf()))
                .andExpect(status().isNotFound());

    }

    @Test
    @WithMockUser(username = "readerUser", roles = {"READER"})
    void reader_ShouldNotBeAbleToUploadNews() throws Exception {
        News mockNews = new News();
        mockNews.setTitle("Unauthorized Article");
        mockNews.setContent("This should fail");

        mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockNews))
                        .with(csrf()))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(username = "readerUser", roles = {"READER"})
    void reader_ShouldNotBeAbleToDeleteNews() throws Exception {

        mockMvc.perform(delete("/api/v1/news/id/" + this.newsId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(username = "readerUser", roles = {"EDITOR"})
    void editor_ShouldBeAbleToUploadNews() throws Exception {
        News mockNews = new News();
        mockNews.setTitle("Authorized Article");
        mockNews.setContent("This should pass");

        mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockNews))
                        .with(csrf()))
                .andExpect(status().isCreated());

    }

    @Test
    @WithMockUser(username = "editorUser", roles = {"EDITOR"})
    void editor_ShouldBeAbleToDeleteNews() throws Exception {

        mockMvc.perform(delete("/api/v1/news/id/" + this.newsId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

    }

    @Test
    @WithMockUser(username = "editorUser", roles = {"EDITOR"})
    void editor_ShouldBeAbleToUpdateNews() throws Exception {

        News news = new News();
        news.setContent("This test should succeed");
        news.setTitle("News added by Reporter");
        news.setAddedBy("reporterUser1");

        news = newsRepository.save(news);
        Long newsId = news.getNewsId();

        News updatePayload = new News();
        updatePayload.setTitle("Updated by Editor");
        updatePayload.setContent("Should pass because Editor is updating");


        mockMvc.perform(post("/api/v1/news/id/" + newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Should pass because Editor is updating"))
                .andExpect(jsonPath("$.title").value("Updated by Editor"));

    }


    @Test
    @WithMockUser(username = "readerUser", roles = {"REPORTER"})
    void reporter_ShouldBeAbleToUploadNews() throws Exception {
        News mockNews = new News();
        mockNews.setTitle("Authorized Article");
        mockNews.setContent("This should pass");

        mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockNews))
                        .with(csrf()))
                .andExpect(status().isCreated());

    }

    @Test
    @WithMockUser(username = "reporterB", roles = {"REPORTER"})
    void reporterB_ShouldNotBeAbleToEditReporterANews() throws Exception {

        News newsByReporterA = new News();
        newsByReporterA.setTitle("Reporter A's News");
        newsByReporterA.setContent("Reporter A's Content");
        newsByReporterA.setAddedBy("reporterA");
        newsByReporterA = newsRepository.save(newsByReporterA);

        System.out.println("DEBUG: Generated News ID is -> " + newsByReporterA.getNewsId());

        Long newsId = newsByReporterA.getNewsId();

        News updatePayload = new News();
        updatePayload.setTitle("Hacked Title by Reporter B");
        updatePayload.setContent("This update should be rejected");

        mockMvc.perform(post("/api/v1/news/id/" + newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload))
                        .with(csrf()))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(username = "reporterA", roles = {"REPORTER"})
    void reporter_ShouldBeAbleToEditTheirOwnNews() throws Exception {

        News news = new News();
        news.setContent("This test should succeed");
        news.setTitle("News by Reporter A");
        news.setAddedBy("reporterA");

        news = newsRepository.save(news);
        Long newsId = news.getNewsId();

        News updatePayload = new News();
        updatePayload.setTitle("Updated by Reporter A");
        updatePayload.setContent("Should pass because owner is updating");


        mockMvc.perform(post("/api/v1/news/id/" + newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "reporterUser", roles = {"REPORTER"})
    void reporter_ShouldNotBeAbleToDeleteNews() throws Exception {

        mockMvc.perform(delete("/api/v1/news/id/" + this.newsId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

    }
}
