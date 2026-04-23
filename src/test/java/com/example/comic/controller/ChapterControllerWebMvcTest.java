package com.example.comic.controller;

import com.example.comic.model.dto.ChapterCommentResponse;
import com.example.comic.model.dto.ChapterPageResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.ChapterCommentService;
import com.example.comic.service.ComicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ChapterController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class ChapterControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComicService comicService;

    @MockBean
    private ChapterCommentService chapterCommentService;

    @Test
    void getPages_shouldReturnStatusDataResponse() throws Exception {
        when(comicService.getChapterPages(1L)).thenReturn(
            List.of(ChapterPageResponse.builder().id(10L).pageNumber(1).imageUrl("img1").build())
        );

        mockMvc
            .perform(get("/chapters/1/pages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].pageNumber").value(1));
    }

    @Test
    void uploadPages_shouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.png", "image/png", "abc".getBytes());
        when(comicService.uploadChapterPages(eq(1L), eq(1), any()))
            .thenReturn(List.of(ChapterPageResponse.builder().id(11L).pageNumber(1).imageUrl("img1").build()));

        mockMvc
            .perform(multipart("/chapters/1/pages").file(file).param("startPageNumber", "1"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data[0].id").value(11L));
    }

    @Test
    void getComments_shouldReturnPagedComments() throws Exception {
        PageDataResponse<ChapterCommentResponse> page = PageDataResponse
            .<ChapterCommentResponse>builder()
            .content(List.of(ChapterCommentResponse.builder().id(1L).content("Hello").createdAt(Instant.now()).build()))
            .pageNo(0)
            .pageSize(20)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
        when(chapterCommentService.getComments(1L, 0, 20)).thenReturn(page);

        mockMvc
            .perform(get("/chapters/1/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].content").value("Hello"));
    }

    @Test
    void createComment_shouldReturnBadRequestWhenBlank() throws Exception {
        mockMvc
            .perform(
                post("/chapters/1/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("content", "   ")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void createComment_shouldReturnCreatedWhenValid() throws Exception {
        when(chapterCommentService.create(eq(1L), any()))
            .thenReturn(ChapterCommentResponse.builder().id(100L).content("Hello").createdAt(Instant.now()).build());

        mockMvc
            .perform(
                post("/chapters/1/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello")))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(100L))
            .andExpect(jsonPath("$.data.content").value("Hello"));
    }

    @Test
    void deletePage_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/chapters/pages/9")).andExpect(status().isNoContent());
        verify(comicService).deleteChapterPage(9L);
    }

    @Test
    void deletePagesByChapter_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/chapters/5/pages")).andExpect(status().isNoContent());
        verify(comicService).deleteChapterPages(5L);
    }
}
