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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository repository;
    @Mock
    private NotificationTemplateMapper mapper;

    private NotificationTemplateService service;

    @BeforeEach
    void setUp() {
        service = new NotificationTemplateService(repository, mapper);
    }

    @Test
    @DisplayName("create lanza DuplicateTemplateException cuando existe un duplicado y no persiste")
    void create_throwsDuplicateTemplateWhenAlreadyExists() {
        String name = "order.confirmed";
        CreateTemplateRequest request = new CreateTemplateRequest(
                name, NotificationType.EMAIL, "Subject", "Body", "{}");
        when(repository.existsByNameAndType(name, NotificationType.EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateTemplateException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create persiste el template con isActive = true por defecto cuando no hay duplicado")
    void create_persistsTemplateWithActiveByDefault() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "order.confirmed", NotificationType.EMAIL, "Subject", "Body", "{}");
        NotificationTemplate entity = NotificationTemplate.builder()
                .name("order.confirmed")
                .type(NotificationType.EMAIL)
                .subject("Subject")
                .body("Body")
                .variables("{}")
                .build();
        when(repository.existsByNameAndType("order.confirmed", NotificationType.EMAIL)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new TemplateResponse(
                UUID.randomUUID(), "order.confirmed", NotificationType.EMAIL,
                "Subject", "Body", "{}", true, null, null));

        service.create(request);

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("update lanza TemplateNotFoundException cuando el id no existe")
    void update_throwsTemplateNotFoundWhenIdMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                new UpdateTemplateRequest("New subject", "New body", "{}")))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    @DisplayName("update cambia solo subject/body/variables y no toca name ni type")
    void update_changesOnlySubjectBodyVariablesKeepingNameAndType() {
        UUID id = UUID.randomUUID();
        NotificationTemplate entity = NotificationTemplate.builder()
                .id(id)
                .name("order.confirmed")
                .type(NotificationType.EMAIL)
                .subject("Old subject")
                .body("Old body")
                .variables("{}")
                .build();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new TemplateResponse(
                id, "order.confirmed", NotificationType.EMAIL,
                "New subject", "New body", "{\"k\":1}", true, null, null));
        doAnswer(invocation -> {
            UpdateTemplateRequest request = invocation.getArgument(0);
            NotificationTemplate target = invocation.getArgument(1);
            target.setSubject(request.subject());
            target.setBody(request.body());
            target.setVariables(request.variables());
            return null;
        }).when(mapper).updateFromRequest(any(), any());

        service.update(id, new UpdateTemplateRequest("New subject", "New body", "{\"k\":1}"));

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(repository).save(captor.capture());
        NotificationTemplate saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("order.confirmed");
        assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(saved.getSubject()).isEqualTo("New subject");
        assertThat(saved.getBody()).isEqualTo("New body");
        assertThat(saved.getVariables()).isEqualTo("{\"k\":1}");
    }

    @Test
    @DisplayName("toggleActive invierte isActive de true a false")
    void toggleActive_flipsActiveToInactive() {
        UUID id = UUID.randomUUID();
        NotificationTemplate entity = NotificationTemplate.builder()
                .id(id).name("t").type(NotificationType.IN_APP).body("b").isActive(true).build();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new TemplateResponse(
                id, "t", NotificationType.IN_APP, null, "b", "{}", false, null, null));

        service.toggleActive(id);

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("toggleActive invierte isActive de false a true")
    void toggleActive_flipsInactiveToActive() {
        UUID id = UUID.randomUUID();
        NotificationTemplate entity = NotificationTemplate.builder()
                .id(id).name("t").type(NotificationType.IN_APP).body("b").isActive(false).build();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new TemplateResponse(
                id, "t", NotificationType.IN_APP, null, "b", "{}", true, null, null));

        service.toggleActive(id);

        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("toggleActive lanza TemplateNotFoundException cuando el id no existe")
    void toggleActive_throwsTemplateNotFoundWhenIdMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleActive(id))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    @DisplayName("findById lanza TemplateNotFoundException cuando el id no existe")
    void findById_throwsTemplateNotFoundWhenIdMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    @DisplayName("findActiveByNameAndType retorna Optional vacío sin lanzar cuando no hay resultado")
    void findActiveByNameAndType_returnsEmptyWhenNotFound() {
        when(repository.findByNameAndTypeAndIsActiveTrue("t", NotificationType.EMAIL))
                .thenReturn(Optional.empty());

        assertThat(service.findActiveByNameAndType("t", NotificationType.EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("findActiveByNameAndType retorna la entidad cuando existe")
    void findActiveByNameAndType_returnsEntityWhenFound() {
        NotificationTemplate entity = NotificationTemplate.builder()
                .id(UUID.randomUUID()).name("t").type(NotificationType.EMAIL).body("b").isActive(true).build();
        when(repository.findByNameAndTypeAndIsActiveTrue("t", NotificationType.EMAIL))
                .thenReturn(Optional.of(entity));

        assertThat(service.findActiveByNameAndType("t", NotificationType.EMAIL)).containsSame(entity);
    }
}