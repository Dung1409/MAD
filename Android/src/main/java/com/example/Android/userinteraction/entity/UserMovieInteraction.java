package com.example.Android.userinteraction.entity;

import java.time.LocalDateTime;

import com.example.Android.Models.Movie;
import com.example.Android.Models.User;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể lưu tương tác người dùng - phim phục vụ gợi ý.
 * Ví dụ: MOVIE_SELECTED (2 điểm), BOOKING_CONFIRMED (5 điểm).
 */
@Entity
@Table(name = "user_movie_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMovieInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    /**
     * Loại tương tác (chuỗi mô tả hành vi).
     */
    @Column(name = "type", length = 30)
    private String type;

    /**
     * Điểm tương tác dùng để tính trọng số gợi ý.
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Thời điểm tạo tương tác (tự động).
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
