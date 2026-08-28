package com.ekko.product_service.config;

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
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_CONFIRMED_QUEUE = "product.order.confirmed.queue";
    public static final String ORDER_CANCELLED_QUEUE = "product.order.cancelled.queue";
    public static final String PRODUCT_DLX = "product.dlx";
    public static final String ORDER_CONFIRMED_DLQ = "product.order.confirmed.dlq";
    public static final String ORDER_CANCELLED_DLQ = "product.order.cancelled.dlq";

    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_CREATED_ROUTING_KEY = "review.created";
    public static final String REVIEW_CREATED_QUEUE = "product.review.created.queue";
    public static final String REVIEW_CREATED_DLQ = "product.review.created.dlq";

    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String PRODUCT_PUBLISHED_ROUTING_KEY = "product.published";
    public static final String PRODUCT_REJECTED_ROUTING_KEY = "product.rejected";
    public static final String PRODUCT_DEACTIVATED_ROUTING_KEY = "product.deactivated";
    public static final String INVENTORY_LOW_STOCK_ROUTING_KEY = "inventory.low_stock";

    public static final String SELLER_EXCHANGE = "seller.exchange";
    public static final String SELLER_STATUS_CHANGED_ROUTING_KEY = "seller.status.changed";
    public static final String SELLER_STATUS_CHANGED_QUEUE = "product.seller.status.changed.queue";
    public static final String SELLER_STATUS_CHANGED_DLQ = "product.seller.status.changed.dlq";


    public static final String SELLER_SLUG_CHANGED_ROUTING_KEY = "seller.slug.changed";
    public static final String SELLER_SLUG_CHANGED_QUEUE = "product.seller.slug.changed.queue";
    public static final String SELLER_SLUG_CHANGED_DLQ = "product.seller.slug.changed.dlq";

    @Bean
    public Queue sellerSlugChangedQueue() {
        return QueueBuilder.durable(SELLER_SLUG_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", PRODUCT_DLX)
                .build();
    }

    @Bean
    public Queue sellerSlugChangedDlq() {
        return QueueBuilder.durable(SELLER_SLUG_CHANGED_DLQ).build();
    }

    @Bean
    public Binding sellerSlugChangedBinding(Queue sellerSlugChangedQueue, DirectExchange sellerExchange) {
        return BindingBuilder
                .bind(sellerSlugChangedQueue)
                .to(sellerExchange)
                .with(SELLER_SLUG_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding sellerSlugChangedDlqBinding(Queue sellerSlugChangedDlq, DirectExchange productDlx) {
        return BindingBuilder
                .bind(sellerSlugChangedDlq)
                .to(productDlx)
                .with(SELLER_SLUG_CHANGED_ROUTING_KEY);
    }


    @Bean
    public DirectExchange sellerExchange() {
        // seller-service already declares seller.exchange as a DirectExchange.
        // Declare it EXACTLY the same (name + type) or RabbitMQ fails startup with 406 PRECONDITION_FAILED.
        return new DirectExchange(SELLER_EXCHANGE, true, false);
    }

    @Bean
    public Queue sellerStatusChangedQueue() {
        return QueueBuilder.durable(SELLER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", PRODUCT_DLX)
                .build();
    }

    @Bean
    public Queue sellerStatusChangedDlq() {
        return QueueBuilder.durable(SELLER_STATUS_CHANGED_DLQ).build();
    }

    @Bean
    public Binding sellerStatusChangedBinding(Queue sellerStatusChangedQueue, DirectExchange sellerExchange) {
        return BindingBuilder
                .bind(sellerStatusChangedQueue)
                .to(sellerExchange)
                .with(SELLER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding sellerStatusChangedDlqBinding(Queue sellerStatusChangedDlq, DirectExchange productDlx) {
        return BindingBuilder
                .bind(sellerStatusChangedDlq)
                .to(productDlx)
                .with(SELLER_STATUS_CHANGED_ROUTING_KEY);
    }


    @Bean
    public DirectExchange orderExchange() {
        // order-service already declares order.exchange as a DirectExchange.
        // Declare it EXACTLY the same (name + type) or RabbitMQ fails startup with 406 PRECONDITION_FAILED.
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange productDlx() {
        // Exclusive DLX owned by product-service.
        return new DirectExchange(PRODUCT_DLX, true, false);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", PRODUCT_DLX)
                .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", PRODUCT_DLX)
                .build();
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return QueueBuilder.durable(ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    public Queue orderCancelledDlq() {
        return QueueBuilder.durable(ORDER_CANCELLED_DLQ).build();
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue, DirectExchange orderExchange) {
        return BindingBuilder
                .bind(orderConfirmedQueue)
                .to(orderExchange)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, DirectExchange orderExchange) {
        return BindingBuilder
                .bind(orderCancelledQueue)
                .to(orderExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedDlqBinding(Queue orderConfirmedDlq, DirectExchange productDlx) {
        // Dead-lettered messages keep their original routing key, so the DLQs bind
        // to product.dlx with the same keys.
        return BindingBuilder
                .bind(orderConfirmedDlq)
                .to(productDlx)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledDlqBinding(Queue orderCancelledDlq, DirectExchange productDlx) {
        return BindingBuilder
                .bind(orderCancelledDlq)
                .to(productDlx)
                .with(ORDER_CANCELLED_ROUTING_KEY);
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
                .withArgument("x-dead-letter-exchange", PRODUCT_DLX)
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
    public Binding reviewCreatedDlqBinding(Queue reviewCreatedDlq, DirectExchange productDlx) {
        return BindingBuilder
                .bind(reviewCreatedDlq)
                .to(productDlx)
                .with(REVIEW_CREATED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange(PRODUCT_EXCHANGE, true, false);
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
        // sends the message to product.dlx (same pattern as seller/review services).
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1000, 2.0, 10000)
                .build());
        return factory;
    }
}