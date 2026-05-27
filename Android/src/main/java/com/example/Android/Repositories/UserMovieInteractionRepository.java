package com.example.Android.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Android.Models.User;
import com.example.Android.userinteraction.entity.UserMovieInteraction;

/**
 * Repository thao tác bảng user_movie_interactions phục vụ gợi ý.
 */
@Repository
public interface UserMovieInteractionRepository extends JpaRepository<UserMovieInteraction, Long> {
    /**
     * Lấy danh sách tương tác theo user, mới nhất trước.
     */
    List<UserMovieInteraction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Xóa toàn bộ tương tác của user (khi xóa tài khoản).
     */
    void deleteByUser(User user);
}
