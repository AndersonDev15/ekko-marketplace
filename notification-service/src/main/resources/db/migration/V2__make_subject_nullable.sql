-- V2: subject nullable en templates y notificaciones IN_APP (no tienen asunto)

ALTER TABLE notification_templates ALTER COLUMN subject DROP NOT NULL;
ALTER TABLE notifications ALTER COLUMN subject DROP NOT NULL;