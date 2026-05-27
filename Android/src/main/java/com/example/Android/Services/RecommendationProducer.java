package com.example.Android.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Producer phát sự kiện gợi ý sang RabbitMQ để xử lý bất đồng bộ.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationProducer {
    RabbitTemplate rabbitTemplate;

    /**
     * Phát sự kiện người dùng chọn thể loại.
     * 
     * @param genreIds danh sách id thể loại.
     * @param userId   định danh người dùng.
     */
    public void sendMessage(List<Long> genreIds, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("genreIds", genreIds);
        message.put("userId", userId);
        message.put("eventType", "GENRES_SELECTED");
        message.put("limit", 20);

        try {
            rabbitTemplate.convertAndSend("recommendationExchange",
                    "recommendationRoutingKey",
                    message);
        } catch (AmqpException ex) {
            // Keep the API flow alive in local/dev when RabbitMQ is unavailable.
            log.error("Failed to publish recommendation message for user {}", userId, ex);
        }
    }

    /**
     * Phát sự kiện người dùng chọn phim.
     * 
     * @param movieId id phim.
     * @param userId  định danh người dùng.
     */
    public void sendMovieSelectedEvent(Long movieId, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("movieId", movieId);
        message.put("userId", userId);
        message.put("eventType", "MOVIE_SELECTED");
        message.put("limit", 20);

        try {
            rabbitTemplate.convertAndSend("recommendationExchange",
                    "recommendationRoutingKey",
                    message);
        } catch (AmqpException ex) {
            log.error("Failed to publish movie selected event for user {}", userId, ex);
        }
    }

}
