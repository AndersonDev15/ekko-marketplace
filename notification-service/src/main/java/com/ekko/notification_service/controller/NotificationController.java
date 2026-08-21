package com.ekko.notification_service.controller;

import com.ekko.notification_service.dto.response.NotificationResponse;
import com.ekko.notification_service.dto.response.UnreadCountResponse;
import com.ekko.notification_service.service.NotificationService;
import com.ekko.notification_service.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> notifications =
                notificationService.findInAppByRecipient(JwtUtils.keycloakId(jwt), pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(notificationService.countUnread(JwtUtils.keycloakId(jwt)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        NotificationResponse response = notificationService.markAsRead(id, JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }
}