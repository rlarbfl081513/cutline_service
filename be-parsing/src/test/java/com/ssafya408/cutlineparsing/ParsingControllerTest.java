package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.api.ParsingController;
import com.ssafya408.cutlineparsing.common.dto.ApiResponse;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.common.security.AuthenticationUtils;
import com.ssafya408.cutlineparsing.dao.UserRepository;
import com.ssafya408.cutlineparsing.service.ParsingService;

import java.time.LocalDate;
import java.util.Optional;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ParsingController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration.class
})
class ParsingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParsingService parsingService;

    @MockBean
    private UserRepository userRepository;

    private MockedStatic<AuthenticationUtils> authUtilsMock;

    @BeforeEach
    void setUp() {
        // AuthenticationUtils 정적 메서드 모킹
        authUtilsMock = Mockito.mockStatic(AuthenticationUtils.class);
        authUtilsMock.when(AuthenticationUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        // 정적 모킹 해제
        if (authUtilsMock != null) {
            authUtilsMock.close();
        }
    }

    @Test
    @DisplayName("POST /{personId} 는 SecurityContext에서 사용자 정보를 추출하여 파싱 서비스를 호출한다")
    void uploadEndpointCallsProcess() throws Exception {
        // Mock User 설정
        User mockUser = new User("test@example.com", "테스트사용자", LocalDate.of(1990, 1, 1), null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "chat.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "dummy".getBytes()
        );

        doNothing().when(parsingService).processWithBatchAutoAnalysis(anyLong(), any(), anyString());

        mockMvc.perform(multipart("/2")
                        .file(file)
                        .cookie(new Cookie("ACCESS_TOKEN", "mock.jwt.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Mockito.verify(parsingService).processWithBatchAutoAnalysis(2L, file, "테스트사용자");
    }

    @Test
    @DisplayName("PUT /{personId} 는 SecurityContext에서 사용자 정보를 추출하여 업데이트 서비스를 호출한다")
    void updateEndpointCallsUpdate() throws Exception {
        // Mock User 설정
        User mockUser = new User("test@example.com", "테스트사용자", LocalDate.of(1990, 1, 1), null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "chat.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "dummy".getBytes()
        );

        doNothing().when(parsingService).update(anyLong(), any(), anyString());

        mockMvc.perform(multipart("/2")
                        .file(file)
                        .cookie(new Cookie("ACCESS_TOKEN", "mock.jwt.token"))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Mockito.verify(parsingService).update(2L, file, "테스트사용자");
    }
}
