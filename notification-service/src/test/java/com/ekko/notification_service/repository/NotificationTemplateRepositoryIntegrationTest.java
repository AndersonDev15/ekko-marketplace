package com.ekko.notification_service.repository;

import com.ekko.notification_service.config.AbstractPostgresIntegrationTest;
import com.ekko.notification_service.entity.NotificationTemplate;
import com.ekko.notification_service.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTemplateRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private NotificationTemplateRepository repository;

    @Test
    @DisplayName("findByNameAndTypeAndIsActiveTrue retorna el template activo existente")
    void findActive_returnsTemplateWhenExistsAndActive() {
        NotificationTemplate saved = repository.save(template("order.confirmed", true));

        Optional<NotificationTemplate> found =
                repository.findByNameAndTypeAndIsActiveTrue("order.confirmed", NotificationType.EMAIL);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("findByNameAndTypeAndIsActiveTrue retorna Optional vacío cuando el template está inactivo")
    void findActive_returnsEmptyWhenTemplateInactive() {
        repository.save(template("order.cancelled", false));

        Optional<NotificationTemplate> found =
                repository.findByNameAndTypeAndIsActiveTrue("order.cancelled", NotificationType.EMAIL);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByNameAndType retorna true/false independientemente del estado de is_active")
    void existsBy_ignoresActiveState() {
        repository.save(template("order.confirmed", true));
        repository.save(template("order.cancelled", false));

        assertThat(repository.existsByNameAndType("order.confirmed", NotificationType.EMAIL)).isTrue();
        assertThat(repository.existsByNameAndType("order.cancelled", NotificationType.EMAIL)).isTrue();
        assertThat(repository.existsByNameAndType("order.unknown", NotificationType.EMAIL)).isFalse();
    }

    @Test
    @DisplayName("el constraint UNIQUE(name, type) de la BD rechaza un duplicado directo")
    void uniqueConstraint_rejectsDuplicateInsert() {
        repository.saveAndFlush(template("order.shipped", true));

        NotificationTemplate duplicate = template("order.shipped", false);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private NotificationTemplate template(String name, boolean isActive) {
        return NotificationTemplate.builder()
                .name(name)
                .type(NotificationType.EMAIL)
                .subject("Subject " + name)
                .body("Body " + name)
                .variables("{}")
                .isActive(isActive)
                .build();
    }
}