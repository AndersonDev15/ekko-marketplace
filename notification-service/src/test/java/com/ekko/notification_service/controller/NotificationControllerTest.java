package com.ekko.notification_service.controller;

import com.ekko.notification_service.config.SecurityConfig;
import com.ekko.notification_service.dto.response.NotificationResponse;
import com.ekko.notification_service.dto.response.UnreadCountResponse;
import com.ekko.notification_service.enums.NotificationStatus;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.NotificationAccessDeniedException;
import com.ekko.notification_service.exception.NotificationNotFoundException;
import com.ekko.notification_service.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID NOTIFICATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor customerAuth() {
        return jwt().jwt(jwt -> jwt
                .subject(CUSTOMER_ID.toString())
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))));
    }

    // ---------- GET /notifications/me ----------

    @Test
    @DisplayName("GET /notifications/me con JWT válido -> 200 y delega con el UUID exacto del subject")
    void getMyNotifications_shouldReturn200AndUseJwtSubject() throws Exception {
        // given
        when(notificationService.findInAppByRecipient(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificationResponse())));

        // when
        // then
        mockMvc.perform(get("/notifications/me")
                        .with(customerAuth())
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(NOTIFICATION_ID.toString()));

        // then
        ArgumentCaptor<UUID> recipientCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService).findInAppByRecipient(recipientCaptor.capture(), any(Pageable.class));
        assertThat(recipientCaptor.getValue()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    @DisplayName("GET /notifications/me pasa page, size y sort al servicio")
    void getMyNotifications_shouldForwardPageableParams() throws Exception {
        // given
        when(notificationService.findInAppByRecipient(eq(CUSTOMER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        mockMvc.perform(get("/notifications/me")
                        .with(customerAuth())
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService).findInAppByRecipient(eq(CUSTOMER_ID), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("GET /notifications/me sin JWT -> 401")
    void getMyNotifications_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/notifications/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /notifications/me/unread-count ----------

    @Test
    @DisplayName("GET /notifications/me/unread-count con JWT válido -> 200 con el conteo")
    void getUnreadCount_shouldReturn200() throws Exception {
        // given
        when(notificationService.countUnread(CUSTOMER_ID)).thenReturn(new UnreadCountResponse(3L));

        // when
        // then
        mockMvc.perform(get("/notifications/me/unread-count").with(customerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("GET /notifications/me/unread-count sin JWT -> 401")
    void getUnreadCount_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/notifications/me/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PATCH /notifications/{id}/read ----------

    @Test
    @DisplayName("PATCH /notifications/{id}/read -> 200 y delega con id y recipient del JWT")
    void markAsRead_shouldReturn200() throws Exception {
        // given
        when(notificationService.markAsRead(NOTIFICATION_ID, CUSTOMER_ID))
                .thenReturn(notificationResponse());

        // when
        // then
        mockMvc.perform(patch("/notifications/{id}/read", NOTIFICATION_ID).with(customerAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()));

        verify(notificationService).markAsRead(NOTIFICATION_ID, CUSTOMER_ID);
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read con notificación inexistente -> 404 NOTIFICATION_NOT_FOUND")
    void markAsRead_shouldReturn404WhenNotificationNotFound() throws Exception {
        // given
        when(notificationService.markAsRead(NOTIFICATION_ID, CUSTOMER_ID))
                .thenThrow(new NotificationNotFoundException(
                        "Notification not found with id: " + NOTIFICATION_ID));

        // when
        // then
        mockMvc.perform(patch("/notifications/{id}/read", NOTIFICATION_ID).with(customerAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Notification not found with id: " + NOTIFICATION_ID))
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read con recipient ajeno -> 403 NOTIFICATION_ACCESS_DENIED")
    void markAsRead_shouldReturn403WhenAccessDenied() throws Exception {
        // given
        when(notificationService.markAsRead(NOTIFICATION_ID, CUSTOMER_ID))
                .thenThrow(new NotificationAccessDeniedException(
                        "Notification " + NOTIFICATION_ID + " does not belong to recipient " + CUSTOMER_ID));

        // when
        // then
        mockMvc.perform(patch("/notifications/{id}/read", NOTIFICATION_ID).with(customerAuth()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_ACCESS_DENIED"))
                .andExpect(jsonPath("$.httpStatus").value(403));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read sin JWT -> 401")
    void markAsRead_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(patch("/notifications/{id}/read", NOTIFICATION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read con id no UUID -> 400")
    void markAsRead_shouldReturn400WhenIdNotUuid() throws Exception {
        mockMvc.perform(patch("/notifications/abc/read").with(customerAuth()))
                .andExpect(status().isBadRequest());
    }

    // ---------- POST /notifications/me/read-all ----------

    @Test
    @DisplayName("POST /notifications/me/read-all -> 204 sin body")
    void markAllAsRead_shouldReturn204() throws Exception {
        // given
        when(notificationService.markAllAsRead(CUSTOMER_ID)).thenReturn(2);

        // when
        // then
        mockMvc.perform(post("/notifications/me/read-all").with(customerAuth()))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(CUSTOMER_ID);
    }

    @Test
    @DisplayName("POST /notifications/me/read-all sin JWT -> 401")
    void markAllAsRead_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(post("/notifications/me/read-all"))
                .andExpect(status().isUnauthorized());
    }

    private NotificationResponse notificationResponse() {
        return new NotificationResponse(
                NOTIFICATION_ID,
                NotificationType.IN_APP,
                "Asunto",
                "Cuerpo",
                NotificationStatus.PENDING,
                null,
                null,
                LocalDateTime.of(2025, 8, 9, 13, 0));
    }
}