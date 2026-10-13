package com.eventhub.api.controller;

import com.eventhub.business.abstracts.CheckInService;
import com.eventhub.config.JwtAuthenticationFilter;
import com.eventhub.config.SecurityConfig;
import com.eventhub.services.JwtService;
import com.eventhub.types.output.CheckInOutput;
import com.eventhub.types.request.CheckInRequest;
import com.eventhub.utilities.result.DataResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// JwtAuthenticationFilter gerçek bean olarak import edilir (bkz. EventControllerTest'teki not) —
// Mockito mock'u filterChain.doFilter(...) çağrısını atlayıp her isteği varsayılan 200 ile bitirirdi.
@WebMvcTest(controllers = CheckInController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CheckInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CheckInService checkInService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void scan_withoutAuthentication_isUnauthorized() throws Exception {
        CheckInRequest request = CheckInRequest.builder().qrToken("some-token").build();

        mockMvc.perform(post("/api/checkins/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void scan_withAuthenticatedUser_delegatesToService() throws Exception {
        when(checkInService.scan(any())).thenReturn(new DataResult<>(CheckInOutput.builder()
                .id(UUID.randomUUID()).build(), true, 200));

        CheckInRequest request = CheckInRequest.builder().qrToken("some-token").build();

        mockMvc.perform(post("/api/checkins/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
