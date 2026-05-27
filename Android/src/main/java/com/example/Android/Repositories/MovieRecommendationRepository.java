package com.example.Android.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Android.Models.User;
import com.example.Android.userinteraction.entity.MovieRecommendation;

/**
 * Repository thao tác bảng movie_recommendations.
 */
@Repository
public interface MovieRecommendationRepository extends JpaRepository<MovieRecommendation, Long> {
    /**
     * Lấy danh sách gợi ý theo user, sắp xếp giảm dần theo điểm.
     */
    List<MovieRecommendation> findByUserOrderByScoreDesc(User user);

    /**
     * Xóa toàn bộ gợi ý của user để tạo snapshot mới.
     */
    void deleteByUser(User user);
}
