-- ENUMS

CREATE TYPE notification_type AS ENUM(
  'EMAIL',
  'IN_APP'
);

CREATE TYPE notification_status AS ENUM(
  'PENDING',
  'SENT',
  'FAILED',
  'CANCELLED'
);

-- TABLES

CREATE TABLE notification_templates(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  type notification_type NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  variables JSONB NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uk_notification_template_name_type
  UNIQUE(name, type)
);

CREATE TABLE notifications(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES notification_templates(id),
    recipient_id UUID NOT NULL,
    recipient_email VARCHAR(255),
    type notification_type NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status notification_status NOT NULL DEFAULT 'PENDING',
    rendered_variables JSONB NOT NULL,
    error_message TEXT,
    metadata JSONB,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()


);

--indexes

CREATE INDEX idx_notifications_recipient_id
    ON notifications(recipient_id);

CREATE INDEX idx_notifications_type
    ON notifications(type);

CREATE INDEX idx_notifications_status
    ON notifications(status);
