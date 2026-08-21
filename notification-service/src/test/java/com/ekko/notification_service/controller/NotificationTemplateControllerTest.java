package com.ekko.notification_service.controller;

import com.ekko.notification_service.config.SecurityConfig;
import com.ekko.notification_service.dto.request.CreateTemplateRequest;
import com.ekko.notification_service.dto.request.UpdateTemplateRequest;
import com.ekko.notification_service.dto.response.TemplateResponse;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.DuplicateTemplateException;
import com.ekko.notification_service.exception.TemplateNotFoundException;
import com.ekko.notification_service.service.NotificationTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationTemplateController.class)
@Import(SecurityConfig.class)
class NotificationTemplateControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEMPLATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private NotificationTemplateService notificationTemplateService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor adminAuth() {
        return bearerAuth(ADMIN_ID, "ADMIN");
    }

    private RequestPostProcessor customerAuth() {
        return bearerAuth(CUSTOMER_ID, "CUSTOMER");
    }

    private RequestPostProcessor bearerAuth(UUID subject, String... roles) {
        return jwt().jwt(jwt -> jwt
                        .subject(subject.toString())
                        .claim("realm_access", Map.of("roles", Arrays.asList(roles))))
                .authorities(jwt -> jwtAuthenticationConverter.convert(jwt).getAuthorities());
    }

    // ---------- POST /admin/notification-templates ----------

    @Test
    @DisplayName("POST /admin/notification-templates como ADMIN -> 201 y delega el request al servicio")
    void createTemplate_shouldReturn201ForAdmin() throws Exception {
        // given
        when(notificationTemplateService.create(any(CreateTemplateRequest.class)))
                .thenReturn(templateResponse());

        // when
        // then
        mockMvc.perform(post("/admin/notification-templates")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "order.confirmed",
                                  "type": "EMAIL",
                                  "subject": "Pedido confirmado",
                                  "body": "Hola {{customerName}}",
                                  "variables": "{}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEMPLATE_ID.toString()));

        ArgumentCaptor<CreateTemplateRequest> captor = ArgumentCaptor.forClass(CreateTemplateRequest.class);
        verify(notificationTemplateService).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("order.confirmed");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.EMAIL);
        assertThat(captor.getValue().body()).isEqualTo("Hola {{customerName}}");
    }

    @Test
    @DisplayName("POST /admin/notification-templates como CUSTOMER -> 403 sin invocar al servicio")
    void createTemplate_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(post("/admin/notification-templates")
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "order.confirmed", "type": "EMAIL", "body": "Body"}
                                """))
                .andExpect(status().isForbidden());

        verify(notificationTemplateService, never()).create(any());
    }

    @Test
    @DisplayName("POST /admin/notification-templates sin JWT -> 401")
    void createTemplate_shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(post("/admin/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "order.confirmed", "type": "EMAIL", "body": "Body"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/notification-templates con nombre duplicado -> 409 DUPLICATE_TEMPLATE")
    void createTemplate_shouldReturn409WhenDuplicate() throws Exception {
        // given
        when(notificationTemplateService.create(any(CreateTemplateRequest.class)))
                .thenThrow(new DuplicateTemplateException(
                        "A notification template with name 'order.confirmed' and type EMAIL already exists"));

        // when
        // then
        mockMvc.perform(post("/admin/notification-templates")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "order.confirmed", "type": "EMAIL", "body": "Body"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_TEMPLATE"))
                .andExpect(jsonPath("$.httpStatus").value(409));
    }

    @Test
    @DisplayName("POST /admin/notification-templates con name en blanco -> 400 y never() en el servicio")
    void createTemplate_shouldReturn400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/admin/notification-templates")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "type": "EMAIL", "body": "Body"}
                                """))
                .andExpect(status().isBadRequest());

        verify(notificationTemplateService, never()).create(any());
    }

    @Test
    @DisplayName("POST /admin/notification-templates con type nulo -> 400 y never() en el servicio")
    void createTemplate_shouldReturn400WhenTypeNull() throws Exception {
        mockMvc.perform(post("/admin/notification-templates")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "order.confirmed", "body": "Body"}
                                """))
                .andExpect(status().isBadRequest());

        verify(notificationTemplateService, never()).create(any());
    }

    // ---------- GET /admin/notification-templates ----------

    @Test
    @DisplayName("GET /admin/notification-templates como ADMIN -> 200 con la lista")
    void listTemplates_shouldReturn200ForAdmin() throws Exception {
        // given
        when(notificationTemplateService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(templateResponse())));

        // when
        // then
        mockMvc.perform(get("/admin/notification-templates").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEMPLATE_ID.toString()));
    }

    @Test
    @DisplayName("GET /admin/notification-templates como CUSTOMER -> 403")
    void listTemplates_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(get("/admin/notification-templates").with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    // ---------- GET /admin/notification-templates/{id} ----------

    @Test
    @DisplayName("GET /admin/notification-templates/{id} como ADMIN -> 200")
    void getTemplate_shouldReturn200ForAdmin() throws Exception {
        // given
        when(notificationTemplateService.findById(TEMPLATE_ID)).thenReturn(templateResponse());

        // when
        // then
        mockMvc.perform(get("/admin/notification-templates/{id}", TEMPLATE_ID).with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEMPLATE_ID.toString()));
    }

    @Test
    @DisplayName("GET /admin/notification-templates/{id} inexistente -> 404 TEMPLATE_NOT_FOUND")
    void getTemplate_shouldReturn404WhenNotFound() throws Exception {
        // given
        when(notificationTemplateService.findById(TEMPLATE_ID))
                .thenThrow(new TemplateNotFoundException(
                        "Notification template not found with id: " + TEMPLATE_ID));

        // when
        // then
        mockMvc.perform(get("/admin/notification-templates/{id}", TEMPLATE_ID).with(adminAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Notification template not found with id: " + TEMPLATE_ID))
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    @Test
    @DisplayName("GET /admin/notification-templates/{id} como CUSTOMER -> 403")
    void getTemplate_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(get("/admin/notification-templates/{id}", TEMPLATE_ID).with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    // ---------- PUT /admin/notification-templates/{id} ----------

    @Test
    @DisplayName("PUT /admin/notification-templates/{id} como ADMIN -> 200 y el DTO capturado no incluye name/type")
    void updateTemplate_shouldReturn200ForAdminAndNotExposeNameType() throws Exception {
        // given
        when(notificationTemplateService.update(eq(TEMPLATE_ID), any(UpdateTemplateRequest.class)))
                .thenReturn(templateResponse());

        // when
        // then
        mockMvc.perform(put("/admin/notification-templates/{id}", TEMPLATE_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject": "Pedido actualizado", "body": "Cuerpo nuevo", "variables": "{}"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEMPLATE_ID.toString()));

        ArgumentCaptor<UpdateTemplateRequest> captor = ArgumentCaptor.forClass(UpdateTemplateRequest.class);
        verify(notificationTemplateService).update(eq(TEMPLATE_ID), captor.capture());
        assertThat(captor.getValue().subject()).isEqualTo("Pedido actualizado");
        assertThat(captor.getValue().body()).isEqualTo("Cuerpo nuevo");

        List<String> components = Arrays.stream(UpdateTemplateRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();
        assertThat(components).containsExactly("subject", "body", "variables");
        assertThat(components).doesNotContain("name", "type");
    }

    @Test
    @DisplayName("PUT /admin/notification-templates/{id} como CUSTOMER -> 403")
    void updateTemplate_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(put("/admin/notification-templates/{id}", TEMPLATE_ID)
                        .with(customerAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Cuerpo"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /admin/notification-templates/{id} inexistente -> 404 TEMPLATE_NOT_FOUND")
    void updateTemplate_shouldReturn404WhenNotFound() throws Exception {
        // given
        when(notificationTemplateService.update(eq(TEMPLATE_ID), any(UpdateTemplateRequest.class)))
                .thenThrow(new TemplateNotFoundException(
                        "Notification template not found with id: " + TEMPLATE_ID));

        // when
        // then
        mockMvc.perform(put("/admin/notification-templates/{id}", TEMPLATE_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Cuerpo"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_NOT_FOUND"))
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    @Test
    @DisplayName("PUT /admin/notification-templates/{id} con body en blanco -> 400 y never() en el servicio")
    void updateTemplate_shouldReturn400WhenBodyBlank() throws Exception {
        mockMvc.perform(put("/admin/notification-templates/{id}", TEMPLATE_ID)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject": "S", "body": "   "}
                                """))
                .andExpect(status().isBadRequest());

        verify(notificationTemplateService, never()).update(any(), any());
    }

    // ---------- PATCH /admin/notification-templates/{id}/toggle-active ----------

    @Test
    @DisplayName("PATCH /admin/notification-templates/{id}/toggle-active como ADMIN -> 200")
    void toggleActive_shouldReturn200ForAdmin() throws Exception {
        // given
        when(notificationTemplateService.toggleActive(TEMPLATE_ID)).thenReturn(templateResponse());

        // when
        // then
        mockMvc.perform(patch("/admin/notification-templates/{id}/toggle-active", TEMPLATE_ID).with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEMPLATE_ID.toString()));

        verify(notificationTemplateService).toggleActive(TEMPLATE_ID);
    }

    @Test
    @DisplayName("PATCH /admin/notification-templates/{id}/toggle-active como CUSTOMER -> 403")
    void toggleActive_shouldReturn403ForCustomer() throws Exception {
        mockMvc.perform(patch("/admin/notification-templates/{id}/toggle-active", TEMPLATE_ID).with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /admin/notification-templates/{id}/toggle-active inexistente -> 404 TEMPLATE_NOT_FOUND")
    void toggleActive_shouldReturn404WhenNotFound() throws Exception {
        // given
        when(notificationTemplateService.toggleActive(TEMPLATE_ID))
                .thenThrow(new TemplateNotFoundException(
                        "Notification template not found with id: " + TEMPLATE_ID));

        // when
        // then
        mockMvc.perform(patch("/admin/notification-templates/{id}/toggle-active", TEMPLATE_ID).with(adminAuth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TEMPLATE_NOT_FOUND"))
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    private TemplateResponse templateResponse() {
        return new TemplateResponse(
                TEMPLATE_ID,
                "order.confirmed",
                NotificationType.EMAIL,
                "Pedido confirmado",
                "Hola {{customerName}}",
                "{}",
                true,
                LocalDateTime.of(2025, 8, 9, 13, 0),
                LocalDateTime.of(2025, 8, 9, 13, 0));
    }
}