package com.ekko.order_service.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateConfigurer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status_changed";

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_COMPLETED_QUEUE = "order-service.payment.completed.queue";
    public static final String PAYMENT_FAILED_QUEUE = "order-service.payment.failed.queue";
    public static final String ORDER_DLX = "order.dlx";
    public static final String PAYMENT_COMPLETED_DLQ = "order-service.payment.completed.dlq";
    public static final String PAYMENT_FAILED_DLQ = "order-service.payment.failed.dlq";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentExchange() {
        // payment-service already declares payment.exchange as a DirectExchange
        // (payment-service RabbitMQConfig.java:33). Declare it EXACTLY the same
        // (name + type) or RabbitMQ fails startup with 406 PRECONDITION_FAILED,
        // and the binding below cannot be established.
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderDlx() {
        // Exclusive DLX owned by order-service.
        return new DirectExchange(ORDER_DLX, true, false);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .build();
    }

    @Bean
    public Queue paymentCompletedDlq() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_DLQ).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .build();
    }

    @Bean
    public Queue paymentFailedDlq() {
        return QueueBuilder.durable(PAYMENT_FAILED_DLQ).build();
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentCompletedQueue)
                .to(paymentExchange)
                .with(PAYMENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentFailedQueue)
                .to(paymentExchange)
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCompletedDlqBinding(Queue paymentCompletedDlq, DirectExchange orderDlx) {
        // Dead-lettered messages keep their original routing key (payment.completed), so the DLQ
        // binds to order.dlx with that same key.
        return BindingBuilder
                .bind(paymentCompletedDlq)
                .to(orderDlx)
                .with(PAYMENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedDlqBinding(Queue paymentFailedDlq, DirectExchange orderDlx) {
        return BindingBuilder
                .bind(paymentFailedDlq)
                .to(orderDlx)
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            RabbitTemplateConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        configurer.configure(rabbitTemplate, connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        // Bounded retries then reject without requeue, so the native dead-lettering sends the
        // message to order.dlx. This avoids the infinite requeue documented in payment-service's
        // OrderCreatedEventListener (no DLQ there) — a pattern payment-service should adopt later.
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1000, 2.0, 10000)
                .build());
        return factory;
    }
}