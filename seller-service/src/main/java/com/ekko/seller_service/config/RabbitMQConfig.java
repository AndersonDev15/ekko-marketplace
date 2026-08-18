package com.ekko.seller_service.config;

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
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CONFIRMED_QUEUE = "seller.order.confirmed.queue";
    public static final String SELLER_DLX = "seller.dlx";
    public static final String ORDER_CONFIRMED_DLQ = "seller.order.confirmed.dlq";

    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_CREATED_ROUTING_KEY = "review.created";
    public static final String REVIEW_CREATED_QUEUE = "seller.review.created.queue";
    public static final String REVIEW_CREATED_DLQ = "seller.review.created.dlq";

    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String PRODUCT_PUBLISHED_ROUTING_KEY = "product.published";
    public static final String PRODUCT_DEACTIVATED_ROUTING_KEY = "product.deactivated";
    public static final String PRODUCT_PUBLISHED_QUEUE = "seller.product.published.queue";
    public static final String PRODUCT_DEACTIVATED_QUEUE = "seller.product.deactivated.queue";
    public static final String PRODUCT_PUBLISHED_DLQ = "seller.product.published.dlq";
    public static final String PRODUCT_DEACTIVATED_DLQ = "seller.product.deactivated.dlq";

    public static final String SELLER_EXCHANGE = "seller.exchange";
    public static final String SELLER_CREATED_ROUTING_KEY = "seller.created";
    public static final String SELLER_STATUS_CHANGED_ROUTING_KEY = "seller.status.changed";
    public static final String SELLER_DOCUMENT_REVIEW_ROUTING_KEY = "seller.document.review";

    @Bean
    public DirectExchange orderExchange() {
        // order-service already declares order.exchange as a DirectExchange.
        // Declare it EXACTLY the same (name + type) or RabbitMQ fails startup with 406 PRECONDITION_FAILED.
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange sellerDlx() {
        // Exclusive DLX owned by seller-service.
        return new DirectExchange(SELLER_DLX, true, false);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", SELLER_DLX)
                .build();
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return QueueBuilder.durable(ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue, DirectExchange orderExchange) {
        return BindingBuilder
                .bind(orderConfirmedQueue)
                .to(orderExchange)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedDlqBinding(Queue orderConfirmedDlq, DirectExchange sellerDlx) {
        // Dead-lettered messages keep their original routing key (order.confirmed),
        // so the DLQ binds to seller.dlx with that same key.
        return BindingBuilder
                .bind(orderConfirmedDlq)
                .to(sellerDlx)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange reviewExchange() {
        // review-service already declares review.exchange as a DirectExchange.
        // Declare it EXACTLY the same (name + type) or RabbitMQ fails startup with 406 PRECONDITION_FAILED.
        return new DirectExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewCreatedQueue() {
        return QueueBuilder.durable(REVIEW_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", SELLER_DLX)
                .build();
    }

    @Bean
    public Queue reviewCreatedDlq() {
        return QueueBuilder.durable(REVIEW_CREATED_DLQ).build();
    }

    @Bean
    public Binding reviewCreatedBinding(Queue reviewCreatedQueue, DirectExchange reviewExchange) {
        return BindingBuilder
                .bind(reviewCreatedQueue)
                .to(reviewExchange)
                .with(REVIEW_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding reviewCreatedDlqBinding(Queue reviewCreatedDlq, DirectExchange sellerDlx) {
        return BindingBuilder
                .bind(reviewCreatedDlq)
                .to(sellerDlx)
                .with(REVIEW_CREATED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange productExchange() {
        // product-service already declares product.exchange as a DirectExchange.
        // Declare it EXACTLY the same (name + type) or RabbitMQ fails startup with 406 PRECONDITION_FAILED.
        return new DirectExchange(PRODUCT_EXCHANGE, true, false);
    }

    @Bean
    public Queue productPublishedQueue() {
        return QueueBuilder.durable(PRODUCT_PUBLISHED_QUEUE)
                .withArgument("x-dead-letter-exchange", SELLER_DLX)
                .build();
    }

    @Bean
    public Queue productPublishedDlq() {
        return QueueBuilder.durable(PRODUCT_PUBLISHED_DLQ).build();
    }

    @Bean
    public Binding productPublishedBinding(Queue productPublishedQueue, DirectExchange productExchange) {
        return BindingBuilder
                .bind(productPublishedQueue)
                .to(productExchange)
                .with(PRODUCT_PUBLISHED_ROUTING_KEY);
    }

    @Bean
    public Binding productPublishedDlqBinding(Queue productPublishedDlq, DirectExchange sellerDlx) {
        return BindingBuilder
                .bind(productPublishedDlq)
                .to(sellerDlx)
                .with(PRODUCT_PUBLISHED_ROUTING_KEY);
    }

    @Bean
    public Queue productDeactivatedQueue() {
        return QueueBuilder.durable(PRODUCT_DEACTIVATED_QUEUE)
                .withArgument("x-dead-letter-exchange", SELLER_DLX)
                .build();
    }

    @Bean
    public Queue productDeactivatedDlq() {
        return QueueBuilder.durable(PRODUCT_DEACTIVATED_DLQ).build();
    }

    @Bean
    public Binding productDeactivatedBinding(Queue productDeactivatedQueue, DirectExchange productExchange) {
        return BindingBuilder
                .bind(productDeactivatedQueue)
                .to(productExchange)
                .with(PRODUCT_DEACTIVATED_ROUTING_KEY);
    }

    @Bean
    public Binding productDeactivatedDlqBinding(Queue productDeactivatedDlq, DirectExchange sellerDlx) {
        return BindingBuilder
                .bind(productDeactivatedDlq)
                .to(sellerDlx)
                .with(PRODUCT_DEACTIVATED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange sellerExchange() {
        return new DirectExchange(SELLER_EXCHANGE, true, false);
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
        // Bounded retries then reject without requeue, so the native dead-lettering
        // sends the message to seller.dlx (same pattern as review-service).
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1000, 2.0, 10000)
                .build());
        return factory;
    }
}