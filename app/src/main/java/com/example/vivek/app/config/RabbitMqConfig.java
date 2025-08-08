package com.example.vivek.app.config;


import com.example.vivek.app.util.CacheUtility;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitMqConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
        public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                                   Jackson2JsonMessageConverter messageConverter) {
            SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            factory.setMessageConverter(messageConverter);
            factory.setPrefetchCount(1);
            factory.setConcurrentConsumers(1);
            factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//            factory.setConcurrentConsumers(4);     // Minimum
//            factory.setMaxConcurrentConsumers(5);  // Maximum
            factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                    .maxAttempts(3)
                    .backOffOptions(1000, 2.0, 10000)
                    .recoverer(new RejectAndDontRequeueRecoverer())
                    .build());
            return factory;
        }

    @Bean
    public Queue myQueue() {
        return new Queue("my-queue");
    }

    @Bean
    public Queue myHighPriorityQueue() {
        return new Queue("my-high-priority-queue");
    }

    @Bean
    public Binding highPriorityBinding() {
        return BindingBuilder
                .bind(myHighPriorityQueue())
                .to(delayedExchange())
                .with("high-routing-key")
                .noargs();
    }


    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");  // Treat like a direct exchange
        return new CustomExchange("my-delayed-exchange", "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(myQueue())
                .to(delayedExchange())
                .with("my-routing-key")
                .noargs();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ApplicationRunner runner(RabbitAdmin rabbitAdmin) {
        CacheUtility.setTaskDivision(false);
        return args -> rabbitAdmin.initialize();
    }

}

