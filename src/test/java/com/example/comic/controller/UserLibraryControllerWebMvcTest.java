package com.example.comic.controller;

import com.example.comic.model.LibraryListType;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.UserLibraryItemResponse;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.LibraryService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = UserLibraryController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class UserLibraryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryService libraryService;

    @Test
    void getLibraries_shouldReturnPagedResponse() throws Exception {
        PageDataResponse<UserLibraryItemResponse> page = PageDataResponse
            .<UserLibraryItemResponse>builder()
            .content(
                List.of(
                    UserLibraryItemResponse
                        .builder()
                        .id(1L)
                        .title("Comic A")
                        .listType(LibraryListType.READING)
                        .savedAt(Instant.parse("2025-01-01T00:00:00Z"))
                        .build()
                )
            )
            .pageNo(0)
            .pageSize(20)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
        when(libraryService.getLibraries(LibraryListType.READING, 0, 20)).thenReturn(page);

        mockMvc
            .perform(get("/user-libraries").param("listType", "READING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("Comic A"));
    }

    @Test
    void upsertLibrary_shouldReturnMessage() throws Exception {
        mockMvc
            .perform(
                post("/user-libraries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("comicId", 1, "listType", "FAVORITE")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Đã cập nhật tủ sách thành công."));

        verify(libraryService).upsertLibrary(any());
    }

    @Test
    void upsertLibrary_shouldReturnBadRequestWhenMissingListType() throws Exception {
        mockMvc
            .perform(
                post("/user-libraries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("comicId", 1)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void remove_shouldReturnMessage() throws Exception {
        mockMvc
            .perform(delete("/user-libraries/comics/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Đã xóa truyện khỏi tủ sách."));

        verify(libraryService).removeFromLibrary(10L);
    }
}
