package com.ekko.notification_service.mapper;

import com.ekko.notification_service.dto.response.NotificationResponse;
import com.ekko.notification_service.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}