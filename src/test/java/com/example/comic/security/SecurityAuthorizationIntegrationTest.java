package com.example.comic.security;

import com.example.comic.model.LibraryListType;
import com.example.comic.model.UserRole;
import com.example.comic.model.dto.AdminUserSummaryResponse;
import com.example.comic.model.dto.ComicSummaryResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.UserLibraryItemResponse;
import com.example.comic.service.AdminService;
import com.example.comic.service.AuthService;
import com.example.comic.service.ChapterCommentService;
import com.example.comic.service.ComicService;
import com.example.comic.service.CurrentUserService;
import com.example.comic.service.LibraryService;
import com.example.comic.service.ReadingHistoryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private AuthService authService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private ComicService comicService;

    @MockBean
    private ChapterCommentService chapterCommentService;

    @MockBean
    private LibraryService libraryService;

    @MockBean
    private ReadingHistoryService readingHistoryService;

    @MockBean
    private AuthCookieService authCookieService;

    @Test
    void publicEndpoint_getComics_shouldBeAccessibleWithoutAuth() throws Exception {
        PageDataResponse<ComicSummaryResponse> page = PageDataResponse
            .<ComicSummaryResponse>builder()
            .content(List.of(ComicSummaryResponse.builder().id(1L).title("Comic A").build()))
            .pageNo(0)
            .pageSize(20)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
        when(comicService.getComics(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc
            .perform(get("/comics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("Comic A"));
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedWithoutAuth() throws Exception {
        mockMvc
            .perform(get("/admin/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.status").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = { "MEMBER" })
    void adminEndpoint_shouldReturnForbiddenForMember() throws Exception {
        mockMvc
            .perform(get("/admin/users"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.status").value("PERMISSION_DENIED"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
    void adminEndpoint_shouldAllowAdmin() throws Exception {
        PageDataResponse<AdminUserSummaryResponse> page = PageDataResponse
            .<AdminUserSummaryResponse>builder()
            .content(List.of(AdminUserSummaryResponse.builder().id(10L).email("u@example.com").build()))
            .pageNo(0)
            .pageSize(20)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
        when(adminService.getUsers(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc
            .perform(get("/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].email").value("u@example.com"));
    }

    @Test
    void chapterCommentCreate_shouldRequireAuthentication() throws Exception {
        mockMvc
            .perform(post("/chapters/1/comments").contentType("application/json").content("{\"content\":\"Hello\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = { "MEMBER" })
    void userLibrary_shouldAllowAuthenticatedUser() throws Exception {
        PageDataResponse<UserLibraryItemResponse> page = PageDataResponse
            .<UserLibraryItemResponse>builder()
            .content(
                List.of(
                    UserLibraryItemResponse
                        .builder()
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
        when(libraryService.getLibraries(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc
            .perform(get("/user-libraries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].title").value("Comic A"));
    }

    @Test
    void authMe_shouldBePublic() throws Exception {
        when(currentUserService.resolveRole()).thenReturn(UserRole.GUEST);

        mockMvc
            .perform(get("/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("GUEST"));
    }

    @Test
    void authRegister_shouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc
            .perform(
                post("/auth/register")
                    .contentType("application/json")
                    .content("{\"email\":\"bad\",\"password\":\"123\",\"fullName\":\"\"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    void authLogin_shouldReturnUnsupportedMediaTypeForTextPlain() throws Exception {
        mockMvc
            .perform(post("/auth/login").contentType("text/plain").content("email=user@example.com"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
    void adminUsers_shouldReturnBadRequestWhenPageTypeInvalid() throws Exception {
        mockMvc
            .perform(get("/admin/users").param("page", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = { "MEMBER" })
    void userLibraries_shouldReturnBadRequestWhenEnumInvalid() throws Exception {
        mockMvc
            .perform(get("/user-libraries").param("listType", "INVALID_ENUM"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.status").value("INVALID_ARGUMENT"));
    }
}
