package com.example.Android.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Android.Models.User;
import com.example.Android.Models.UserGenrePreference;

/**
 * Repository thao tác bảng user_genre_preferences cho fallback gợi ý.
 */
@Repository
public interface UserGenrePreferenceRepository extends JpaRepository<UserGenrePreference, Long> {
    /**
     * Lấy danh sách preference theo user.
     */
    List<UserGenrePreference> findByUser(User user);

    /**
     * Xóa preference theo user (khi xóa tài khoản).
     */
    void deleteByUser(User user);
}
