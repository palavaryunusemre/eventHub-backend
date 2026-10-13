package com.eventhub.api.controller;

import com.eventhub.business.abstracts.EventService;
import com.eventhub.config.JwtAuthenticationFilter;
import com.eventhub.config.SecurityConfig;
import com.eventhub.services.JwtService;
import com.eventhub.types.output.EventSummaryOutput;
import com.eventhub.types.request.EventCreateRequest;
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

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// JwtAuthenticationFilter gerçek (mock değil) bean olarak import edilir: JwtAuthenticationFilter'ı
// Mockito mock'u ile değiştirmek doFilter'ı no-op yapıp filterChain.doFilter(...) çağrısını atlardı —
// bu da her isteği (401 beklenen senaryolar dahil) varsayılan 200 ile sonuçlandırırdı.
@WebMvcTest(controllers = EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void discover_isPublic_returnsOkWithoutAuth() throws Exception {
        when(eventService.discover(any(), any())).thenReturn(new DataResult<>(List.<EventSummaryOutput>of(), true, 200));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk());
    }

    @Test
    void create_withoutAuthentication_isUnauthorized() throws Exception {
        EventCreateRequest request = EventCreateRequest.builder()
                .title("Yeni Etkinlik")
                .visibility("PUBLIC")
                .format("ONLINE")
                .startAt(new Date())
                .endAt(new Date())
                .build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_withAuthenticatedUser_delegatesToService() throws Exception {
        when(eventService.create(any())).thenReturn(new DataResult<>(java.util.UUID.randomUUID(), true, 200));

        EventCreateRequest request = EventCreateRequest.builder()
                .title("Yeni Etkinlik")
                .visibility("PUBLIC")
                .format("ONLINE")
                .startAt(new Date())
                .endAt(new Date())
                .build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
