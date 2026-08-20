package com.ekko.notification_service.service;

import com.ekko.notification_service.dto.request.CreateTemplateRequest;
import com.ekko.notification_service.dto.request.UpdateTemplateRequest;
import com.ekko.notification_service.dto.response.TemplateResponse;
import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationType;
import com.ekko.notification_service.exception.DuplicateTemplateException;
import com.ekko.notification_service.exception.TemplateNotFoundException;
import com.ekko.notification_service.mapper.NotificationTemplateMapper;
import com.ekko.notification_service.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;
    private final NotificationTemplateMapper mapper;

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        if (repository.existsByNameAndType(request.name(), request.type())) {
            throw new DuplicateTemplateException(
                    "A notification template with name '" + request.name() + "' and type " + request.type()
                            + " already exists");
        }
        NotificationTemplate entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public TemplateResponse update(UUID id, UpdateTemplateRequest request) {
        NotificationTemplate entity = findEntity(id);
        mapper.updateFromRequest(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public TemplateResponse toggleActive(UUID id) {
        NotificationTemplate entity = findEntity(id);
        entity.setIsActive(!entity.getIsActive());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<TemplateResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TemplateResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> findActiveByNameAndType(String name, NotificationType type) {
        return repository.findByNameAndTypeAndIsActiveTrue(name, type);
    }

    private NotificationTemplate findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Notification template not found with id: " + id));
    }
}