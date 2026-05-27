package com.example.Android.Services;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer nhận message gợi ý và kích hoạt xử lý tạo snapshot.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationConsumer {
    private final RecommendationService recommendationService;

    /**
     * Nhận message từ queue, parse dữ liệu và gọi xử lý gợi ý.
     * @param message payload map từ RabbitMQ.
     */
    @RabbitListener(queues = "recommendationQueue")
    public void receiveMessage(Map<String, Object> message) {
        String userId = asString(message.get("userId"));
        String eventType = asString(message.get("eventType"));
        Integer limit = asInteger(message.get("limit"), 20);

        if (userId == null || userId.isBlank()) {
            log.warn("Skip recommendation event because userId is missing: {}", message);
            return;
        }

        log.info("Received event {} for user {}", eventType, userId);

        try {
            recommendationService.processRecommendationEvent(userId, limit);
            log.info("Async recommendation processing completed for user {}", userId);
        } catch (Exception ex) {
            log.error("Async recommendation processing failed for user {}", userId, ex);
        }
    }

    /**
     * Chuyển object sang chuỗi an toàn.
     */
    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * Chuyển object sang số nguyên an toàn, fallback mặc định khi lỗi.
     */
    private Integer asInteger(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
