package com.eventhub.api.controller;

import com.eventhub.business.abstracts.RegistrationService;
import com.eventhub.config.JwtAuthenticationFilter;
import com.eventhub.config.SecurityConfig;
import com.eventhub.services.JwtService;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// JwtAuthenticationFilter gerçek bean olarak import edilir (bkz. EventControllerTest'teki not) —
// Mockito mock'u filterChain.doFilter(...) çağrısını atlayıp her isteği varsayılan 200 ile bitirirdi.
@WebMvcTest(controllers = RegistrationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void register_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/events/" + UUID.randomUUID() + "/register"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void register_withAuthenticatedUser_delegatesToService() throws Exception {
        when(registrationService.register(any())).thenReturn(new Result<>(true, 200, "Kayıt başarılı."));

        mockMvc.perform(post("/api/events/" + UUID.randomUUID() + "/register"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMine_withAuthenticatedUser_returnsOk() throws Exception {
        when(registrationService.getMine()).thenReturn(new DataResult<>(List.of(), true, 200));

        mockMvc.perform(get("/api/registrations/mine"))
                .andExpect(status().isOk());
    }
}
