package com.example.comic.controller;

import com.example.comic.model.dto.PageDetailResponse;
import com.example.comic.model.dto.PageImagesResponse;
import com.example.comic.exception.NotFoundException;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.PageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PageController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
@AutoConfigureMockMvc(addFilters = false)
class PageControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockBean
    private PageService pageService;

    @Test
    void getPageDetail_shouldReturnOkWithMergedData() throws Exception {
        ArrayNode bubbles = objectMapper.createArrayNode();
        bubbles.add(objectMapper.createObjectNode().put("id", 1).put("original_text", "えー").put("full_translation",
                "À này"));

        when(pageService.getPageDetail(1L, "vi")).thenReturn(
                PageDetailResponse.builder()
                        .pageId(1L)
                        .chapterId(10L)
                        .pageNumber(5)
                        .images(PageImagesResponse.builder()
                                .originalUrl("http://cdn/pages/1.png")
                                .inpaintedUrl("http://cdn/cleaned/1.png")
                                .build())
                        .bubbles(bubbles)
                        .build());

        mockMvc
                .perform(get("/pages/1").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.pageId").value(1))
                .andExpect(jsonPath("$.data.chapterId").value(10))
                .andExpect(jsonPath("$.data.pageNumber").value(5))
                .andExpect(jsonPath("$.data.images.originalUrl").value("http://cdn/pages/1.png"))
                .andExpect(jsonPath("$.data.images.inpaintedUrl").value("http://cdn/cleaned/1.png"))
                .andExpect(jsonPath("$.data.bubbles[0].full_translation").value("À này"));
    }

    @Test
    void getPageDetail_shouldReturn400WhenLangMissing() throws Exception {
        mockMvc
                .perform(get("/pages/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPageDetail_shouldReturn404WhenPageNotFound() throws Exception {
        when(pageService.getPageDetail(99L, "vi")).thenThrow(new NotFoundException("Không tìm thấy trang truyện."));

        mockMvc
                .perform(get("/pages/99").param("lang", "vi"))
                .andExpect(status().isNotFound());
    }
}
