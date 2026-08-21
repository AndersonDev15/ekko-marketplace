package com.ekko.notification_service.config;

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

    public static final String SELLER_EXCHANGE = "seller.exchange";
    public static final String SELLER_CREATED_ROUTING_KEY = "seller.created";
    public static final String SELLER_STATUS_CHANGED_ROUTING_KEY = "seller.status.changed";
    public static final String SELLER_DOCUMENT_REVIEW_ROUTING_KEY = "seller.document.review";

    public static final String SELLER_CREATED_QUEUE = "notification-service.seller.created.queue";
    public static final String SELLER_CREATED_DLQ = "notification-service.seller.created.dlq";
    public static final String SELLER_STATUS_CHANGED_QUEUE = "notification-service.seller.status.changed.queue";
    public static final String SELLER_STATUS_CHANGED_DLQ = "notification-service.seller.status.changed.dlq";
    public static final String SELLER_DOCUMENT_REVIEW_QUEUE = "notification-service.seller.document.review.queue";
    public static final String SELLER_DOCUMENT_REVIEW_DLQ = "notification-service.seller.document.review.dlq";

    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String PRODUCT_PUBLISHED_ROUTING_KEY = "product.published";
    public static final String PRODUCT_REJECTED_ROUTING_KEY = "product.rejected";
    public static final String PRODUCT_DEACTIVATED_ROUTING_KEY = "product.deactivated";
    public static final String INVENTORY_LOW_STOCK_ROUTING_KEY = "inventory.low_stock";

    public static final String PRODUCT_PUBLISHED_QUEUE = "notification-service.product.published.queue";
    public static final String PRODUCT_PUBLISHED_DLQ = "notification-service.product.published.dlq";
    public static final String PRODUCT_REJECTED_QUEUE = "notification-service.product.rejected.queue";
    public static final String PRODUCT_REJECTED_DLQ = "notification-service.product.rejected.dlq";
    public static final String PRODUCT_DEACTIVATED_QUEUE = "notification-service.product.deactivated.queue";
    public static final String PRODUCT_DEACTIVATED_DLQ = "notification-service.product.deactivated.dlq";
    public static final String INVENTORY_LOW_STOCK_QUEUE = "notification-service.inventory.low_stock.queue";
    public static final String INVENTORY_LOW_STOCK_DLQ = "notification-service.inventory.low_stock.dlq";

    public static final String NOTIFICATION_DLX = "notification.dlx";

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status_changed";

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_CREATED_ROUTING_KEY = "review.created";

    public static final String REVIEW_CREATED_QUEUE = "notification-service.review.created.queue";
    public static final String REVIEW_CREATED_DLQ = "notification-service.review.created.dlq";

    public static final String PAYMENT_FAILED_QUEUE = "notification-service.payment.failed.queue";
    public static final String PAYMENT_FAILED_DLQ = "notification-service.payment.failed.dlq";

    public static final String ORDER_CREATED_QUEUE = "notification-service.order.created.queue";
    public static final String ORDER_CREATED_DLQ = "notification-service.order.created.dlq";
    public static final String ORDER_CONFIRMED_QUEUE = "notification-service.order.confirmed.queue";
    public static final String ORDER_CONFIRMED_DLQ = "notification-service.order.confirmed.dlq";
    public static final String ORDER_CANCELLED_QUEUE = "notification-service.order.cancelled.queue";
    public static final String ORDER_CANCELLED_DLQ = "notification-service.order.cancelled.dlq";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "notification-service.order.status_changed.queue";
    public static final String ORDER_STATUS_CHANGED_DLQ = "notification-service.order.status_changed.dlq";

    @Bean
    public DirectExchange notificationDlx() {
        // Exclusive DLX owned by notification-service.
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue sellerCreatedQueue() {
        return QueueBuilder.durable(SELLER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue sellerCreatedDlq() {
        return QueueBuilder.durable(SELLER_CREATED_DLQ).build();
    }

    @Bean
    public Queue sellerStatusChangedQueue() {
        return QueueBuilder.durable(SELLER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue sellerStatusChangedDlq() {
        return QueueBuilder.durable(SELLER_STATUS_CHANGED_DLQ).build();
    }

    @Bean
    public Queue sellerDocumentReviewQueue() {
        return QueueBuilder.durable(SELLER_DOCUMENT_REVIEW_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue sellerDocumentReviewDlq() {
        return QueueBuilder.durable(SELLER_DOCUMENT_REVIEW_DLQ).build();
    }

    @Bean
    public Binding sellerCreatedBinding(Queue sellerCreatedQueue) {
        // seller.exchange is owned and declared by seller-service; notification-service is only a
        // consumer, so it binds by exchange name without declaring it.
        return new Binding(
                SELLER_CREATED_QUEUE,
                Binding.DestinationType.QUEUE,
                SELLER_EXCHANGE,
                SELLER_CREATED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding sellerStatusChangedBinding(Queue sellerStatusChangedQueue) {
        return new Binding(
                SELLER_STATUS_CHANGED_QUEUE,
                Binding.DestinationType.QUEUE,
                SELLER_EXCHANGE,
                SELLER_STATUS_CHANGED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding sellerDocumentReviewBinding(Queue sellerDocumentReviewQueue) {
        return new Binding(
                SELLER_DOCUMENT_REVIEW_QUEUE,
                Binding.DestinationType.QUEUE,
                SELLER_EXCHANGE,
                SELLER_DOCUMENT_REVIEW_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding sellerCreatedDlqBinding(Queue sellerCreatedDlq, DirectExchange notificationDlx) {
        // Dead-lettered messages keep their original routing key (seller.created), so the DLQ
        // binds to notification.dlx with that same key.
        return BindingBuilder
                .bind(sellerCreatedDlq)
                .to(notificationDlx)
                .with(SELLER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding sellerStatusChangedDlqBinding(Queue sellerStatusChangedDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(sellerStatusChangedDlq)
                .to(notificationDlx)
                .with(SELLER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding sellerDocumentReviewDlqBinding(Queue sellerDocumentReviewDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(sellerDocumentReviewDlq)
                .to(notificationDlx)
                .with(SELLER_DOCUMENT_REVIEW_ROUTING_KEY);
    }

    @Bean
    public Queue productPublishedQueue() {
        return QueueBuilder.durable(PRODUCT_PUBLISHED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue productPublishedDlq() {
        return QueueBuilder.durable(PRODUCT_PUBLISHED_DLQ).build();
    }

    @Bean
    public Queue productRejectedQueue() {
        return QueueBuilder.durable(PRODUCT_REJECTED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue productRejectedDlq() {
        return QueueBuilder.durable(PRODUCT_REJECTED_DLQ).build();
    }

    @Bean
    public Queue productDeactivatedQueue() {
        return QueueBuilder.durable(PRODUCT_DEACTIVATED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue productDeactivatedDlq() {
        return QueueBuilder.durable(PRODUCT_DEACTIVATED_DLQ).build();
    }

    @Bean
    public Queue inventoryLowStockQueue() {
        return QueueBuilder.durable(INVENTORY_LOW_STOCK_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue inventoryLowStockDlq() {
        return QueueBuilder.durable(INVENTORY_LOW_STOCK_DLQ).build();
    }

    @Bean
    public Binding productPublishedBinding(Queue productPublishedQueue) {
        // product.exchange is owned and declared by product-service; notification-service is only a
        // consumer, so it binds by exchange name without declaring it.
        return new Binding(
                PRODUCT_PUBLISHED_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_EXCHANGE,
                PRODUCT_PUBLISHED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding productRejectedBinding(Queue productRejectedQueue) {
        return new Binding(
                PRODUCT_REJECTED_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_EXCHANGE,
                PRODUCT_REJECTED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding productDeactivatedBinding(Queue productDeactivatedQueue) {
        return new Binding(
                PRODUCT_DEACTIVATED_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_EXCHANGE,
                PRODUCT_DEACTIVATED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding inventoryLowStockBinding(Queue inventoryLowStockQueue) {
        return new Binding(
                INVENTORY_LOW_STOCK_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_EXCHANGE,
                INVENTORY_LOW_STOCK_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding productPublishedDlqBinding(Queue productPublishedDlq, DirectExchange notificationDlx) {
        // Dead-lettered messages keep their original routing key (product.published), so the DLQ
        // binds to notification.dlx with that same key.
        return BindingBuilder
                .bind(productPublishedDlq)
                .to(notificationDlx)
                .with(PRODUCT_PUBLISHED_ROUTING_KEY);
    }

    @Bean
    public Binding productRejectedDlqBinding(Queue productRejectedDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(productRejectedDlq)
                .to(notificationDlx)
                .with(PRODUCT_REJECTED_ROUTING_KEY);
    }

    @Bean
    public Binding productDeactivatedDlqBinding(Queue productDeactivatedDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(productDeactivatedDlq)
                .to(notificationDlx)
                .with(PRODUCT_DEACTIVATED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryLowStockDlqBinding(Queue inventoryLowStockDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(inventoryLowStockDlq)
                .to(notificationDlx)
                .with(INVENTORY_LOW_STOCK_ROUTING_KEY);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return QueueBuilder.durable(ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue orderCancelledDlq() {
        return QueueBuilder.durable(ORDER_CANCELLED_DLQ).build();
    }

    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder.durable(ORDER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue orderStatusChangedDlq() {
        return QueueBuilder.durable(ORDER_STATUS_CHANGED_DLQ).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue) {
        // order.exchange is owned and declared by order-service; notification-service is only a
        // consumer, so it binds by exchange name without declaring it.
        return new Binding(
                ORDER_CREATED_QUEUE,
                Binding.DestinationType.QUEUE,
                ORDER_EXCHANGE,
                ORDER_CREATED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue) {
        return new Binding(
                ORDER_CONFIRMED_QUEUE,
                Binding.DestinationType.QUEUE,
                ORDER_EXCHANGE,
                ORDER_CONFIRMED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue) {
        return new Binding(
                ORDER_CANCELLED_QUEUE,
                Binding.DestinationType.QUEUE,
                ORDER_EXCHANGE,
                ORDER_CANCELLED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding orderStatusChangedBinding(Queue orderStatusChangedQueue) {
        return new Binding(
                ORDER_STATUS_CHANGED_QUEUE,
                Binding.DestinationType.QUEUE,
                ORDER_EXCHANGE,
                ORDER_STATUS_CHANGED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding orderCreatedDlqBinding(Queue orderCreatedDlq, DirectExchange notificationDlx) {
        // Dead-lettered messages keep their original routing key (order.created), so the DLQ
        // binds to notification.dlx with that same key.
        return BindingBuilder
                .bind(orderCreatedDlq)
                .to(notificationDlx)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedDlqBinding(Queue orderConfirmedDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(orderConfirmedDlq)
                .to(notificationDlx)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledDlqBinding(Queue orderCancelledDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(orderCancelledDlq)
                .to(notificationDlx)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusChangedDlqBinding(Queue orderStatusChangedDlq, DirectExchange notificationDlx) {
        return BindingBuilder
                .bind(orderStatusChangedDlq)
                .to(notificationDlx)
                .with(ORDER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue paymentFailedDlq() {
        return QueueBuilder.durable(PAYMENT_FAILED_DLQ).build();
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue) {
        // payment.exchange is owned and declared by payment-service; notification-service is only a
        // consumer, so it binds by exchange name without declaring it.
        return new Binding(
                PAYMENT_FAILED_QUEUE,
                Binding.DestinationType.QUEUE,
                PAYMENT_EXCHANGE,
                PAYMENT_FAILED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding paymentFailedDlqBinding(Queue paymentFailedDlq, DirectExchange notificationDlx) {
        // Dead-lettered messages keep their original routing key (payment.failed), so the DLQ
        // binds to notification.dlx with that same key.
        return BindingBuilder
                .bind(paymentFailedDlq)
                .to(notificationDlx)
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Queue reviewCreatedQueue() {
        return QueueBuilder.durable(REVIEW_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .build();
    }

    @Bean
    public Queue reviewCreatedDlq() {
        return QueueBuilder.durable(REVIEW_CREATED_DLQ).build();
    }

    @Bean
    public Binding reviewCreatedBinding(Queue reviewCreatedQueue) {
        // review.exchange is owned and declared by review-service; notification-service is only a
        // consumer, so it binds by exchange name without declaring it.
        return new Binding(
                REVIEW_CREATED_QUEUE,
                Binding.DestinationType.QUEUE,
                REVIEW_EXCHANGE,
                REVIEW_CREATED_ROUTING_KEY,
                null);
    }

    @Bean
    public Binding reviewCreatedDlqBinding(Queue reviewCreatedDlq, DirectExchange notificationDlx) {
        // Dead-lettered messages keep their original routing key (review.created), so the DLQ
        // binds to notification.dlx with that same key.
        return BindingBuilder
                .bind(reviewCreatedDlq)
                .to(notificationDlx)
                .with(REVIEW_CREATED_ROUTING_KEY);
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
        // message to notification.dlx (same pattern as seller-service).
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1000, 2.0, 10000)
                .build());
        return factory;
    }
}