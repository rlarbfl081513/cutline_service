package com.ssafya408.cutlineparsing.service.dto;

public record TopicPayload(
        String topic,
        Integer count
) {
    public static TopicPayload of(String topic, Integer count) {
        return new TopicPayload(topic, count);
    }
}
