package com.example.comic.controller;

import com.example.comic.model.dto.AdminDashboardSummaryResponse;
import com.example.comic.model.dto.AdminUserSummaryResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.security.JwtAuthenticationFilter;
import com.example.comic.security.SecurityConfiguration;
import com.example.comic.service.AdminService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = AdminController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @Test
    void getUsers_shouldReturnPagedResponse() throws Exception {
        PageDataResponse<AdminUserSummaryResponse> page = PageDataResponse
            .<AdminUserSummaryResponse>builder()
            .content(List.of(AdminUserSummaryResponse.builder().id(1L).email("user@example.com").fullName("User").build()))
            .pageNo(0)
            .pageSize(20)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
        when(adminService.getUsers("john", 0, 20)).thenReturn(page);

        mockMvc
            .perform(get("/admin/users").param("keyword", "john"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].email").value("user@example.com"));
    }

    @Test
    void updateUserStatus_shouldReturnUpdatedUser() throws Exception {
        when(adminService.updateUserStatus(1L, "LOCKED"))
            .thenReturn(AdminUserSummaryResponse.builder().id(1L).status("LOCKED").build());

        mockMvc
            .perform(
                patch("/admin/users/1/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("status", "LOCKED")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void updateUserRole_shouldReturnBadRequestWhenBlankRole() throws Exception {
        mockMvc
            .perform(
                patch("/admin/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("role", "  ")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void updateUserRole_shouldReturnUpdatedUser() throws Exception {
        when(adminService.updateUserRole(1L, "ADMIN"))
            .thenReturn(AdminUserSummaryResponse.builder().id(1L).role("ADMIN").build());

        mockMvc
            .perform(
                patch("/admin/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("role", "ADMIN")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void updateUserStatus_shouldReturnBadRequestWhenBlankStatus() throws Exception {
        when(adminService.updateUserStatus(1L, "  ")).thenThrow(new IllegalArgumentException("invalid"));

        mockMvc
            .perform(
                patch("/admin/users/1/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(java.util.Map.of("status", "  ")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void getDashboardSummary_shouldReturnData() throws Exception {
        when(adminService.getDashboardSummary()).thenReturn(AdminDashboardSummaryResponse.builder().totalUsers(100).build());

        mockMvc
            .perform(get("/admin/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalUsers").value(100));
    }
}
