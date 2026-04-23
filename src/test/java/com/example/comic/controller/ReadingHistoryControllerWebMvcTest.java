package com.example.comic.controller;

import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.ReadingHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ReadingHistoryController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class ReadingHistoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReadingHistoryService readingHistoryService;

    @Test
    void getByComicId_shouldReturnDataResponse() throws Exception {
        when(readingHistoryService.getByComicId(1L))
            .thenReturn(ReadingHistoryResponse.builder().comicId(1L).chapterId(3L).lastPageRead(9).updatedAt(Instant.now()).build());

        mockMvc
            .perform(get("/reading-histories/comics/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.comicId").value(1))
            .andExpect(jsonPath("$.data.chapterId").value(3));
    }

    @Test
    void sync_shouldReturnServiceObject() throws Exception {
        when(readingHistoryService.sync(any())).thenReturn(MessageResponse.builder().message("Tiến độ đọc đã được lưu.").build());

        mockMvc
            .perform(
                put("/reading-histories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            java.util.Map.of(
                                "comicId",
                                1,
                                "chapterId",
                                3,
                                "lastPageRead",
                                9,
                                "clientUpdatedAt",
                                "2025-01-01T00:00:00Z"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Tiến độ đọc đã được lưu."));

        verify(readingHistoryService).sync(any());
    }

    @Test
    void sync_shouldReturnBadRequestWhenMissingRequiredFields() throws Exception {
        mockMvc
            .perform(
                put("/reading-histories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("comicId", 1)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }
}
