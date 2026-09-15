package com.ekko.notification_service.controller;

import com.ekko.notification_service.dto.request.CreateTemplateRequest;
import com.ekko.notification_service.dto.request.UpdateTemplateRequest;
import com.ekko.notification_service.dto.response.TemplateResponse;
import com.ekko.notification_service.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/notification-templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Admin endpoints for managing notification templates")
@SecurityRequirement(name = "bearerAuth")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @Operation(summary = "Create notification template", description = "Creates a new notification template (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Template created successfully",
                    content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Conflict - template with same name and type already exists")
    })
    @PostMapping
    public ResponseEntity<TemplateResponse> create(
            @Parameter(description = "Template creation request") @Valid @RequestBody CreateTemplateRequest request) {
        TemplateResponse response = notificationTemplateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List all notification templates", description = "Returns a paginated list of all notification templates (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> findAll(
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationTemplateService.findAll(pageable));
    }

    @Operation(summary = "Get notification template by ID", description = "Returns a notification template by its ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> findById(
            @Parameter(description = "Template ID") @PathVariable UUID id) {
        return ResponseEntity.ok(notificationTemplateService.findById(id));
    }

    @Operation(summary = "Update notification template", description = "Updates an existing notification template (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template updated successfully",
                    content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - template with same name and type already exists")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(
            @Parameter(description = "Template ID") @PathVariable UUID id,
            @Parameter(description = "Template update request") @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(notificationTemplateService.update(id, request));
    }

    @Operation(summary = "Toggle template active status", description = "Toggles the active status of a notification template (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template active status toggled successfully",
                    content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<TemplateResponse> toggleActive(
            @Parameter(description = "Template ID") @PathVariable UUID id) {
        return ResponseEntity.ok(notificationTemplateService.toggleActive(id));
    }
}