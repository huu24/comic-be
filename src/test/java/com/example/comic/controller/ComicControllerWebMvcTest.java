package com.example.comic.controller;

import com.example.comic.model.dto.ComicCreateResponse;
import com.example.comic.model.dto.ComicRatingResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.ComicSummaryResponse;
import com.example.comic.model.dto.ChapterCreateResponse;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.ComicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ComicController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class ComicControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComicService comicService;

    @Test
    void createComic_shouldReturnCreatedResponse() throws Exception {
        when(comicService.createComic(any()))
            .thenReturn(ComicCreateResponse.builder().id(10L).title("One Piece").build());

        mockMvc
            .perform(
                post("/comics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of("title", "One Piece", "format", "MANGA", "status", "ACTIVE")
                        )
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(10L))
            .andExpect(jsonPath("$.data.title").value("One Piece"));
    }

    @Test
    void createComic_shouldReturnBadRequestWhenInvalidPayload() throws Exception {
        mockMvc
            .perform(
                post("/comics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("title", "", "format", "")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void createChapter_shouldReturnCreatedResponse() throws Exception {
        when(comicService.createChapter(eq(10L), any()))
            .thenReturn(ChapterCreateResponse.builder().id(77L).comicId(10L).chapterNumber(3).title("Chap 3").build());

        mockMvc
            .perform(
                post("/comics/10/chapters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("chapterNumber", 3, "title", "Chap 3")))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(77L))
            .andExpect(jsonPath("$.data.chapterNumber").value(3));
    }

    @Test
    void getComics_shouldReturnPagedData() throws Exception {
        PageDataResponse<ComicSummaryResponse> page = PageDataResponse
            .<ComicSummaryResponse>builder()
            .content(List.of(ComicSummaryResponse.builder().id(1L).title("A").build()))
            .pageNo(0)
            .pageSize(20)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
        when(comicService.getComics("key", null, null, null, 0, 20)).thenReturn(page);

        mockMvc
            .perform(get("/comics").param("keyword", "key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("A"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void rateComic_shouldReturnBadRequestWhenScoreOutOfRange() throws Exception {
        mockMvc
            .perform(
                put("/comics/1/ratings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("score", 11)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void rateComic_shouldReturnSuccessResponse() throws Exception {
        when(comicService.rateComic(1L, 8)).thenReturn(ComicRatingResponse.builder().newAverageRating(4.5).totalRatings(100L).build());

        mockMvc
            .perform(
                put("/comics/1/ratings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("score", 8)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Đánh giá thành công."))
            .andExpect(jsonPath("$.data.newAverageRating").value(4.5));

        verify(comicService).rateComic(eq(1L), eq(8));
    }
}
