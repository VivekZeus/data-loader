package com.example.vivek.app.util;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Message;

public class DelayPostProcessor implements MessagePostProcessor {
    private final long delayMs;

    public DelayPostProcessor(long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public Message postProcessMessage(Message message) {
        message.getMessageProperties().setExpiration(String.valueOf(delayMs));
        return message;
    }
}
