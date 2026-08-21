package com.ekko.notification_service.controller;

import com.ekko.notification_service.dto.request.CreateTemplateRequest;
import com.ekko.notification_service.dto.request.UpdateTemplateRequest;
import com.ekko.notification_service.dto.response.TemplateResponse;
import com.ekko.notification_service.service.NotificationTemplateService;
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
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest request) {
        TemplateResponse response = notificationTemplateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationTemplateService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationTemplateService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(notificationTemplateService.update(id, request));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<TemplateResponse> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationTemplateService.toggleActive(id));
    }
}