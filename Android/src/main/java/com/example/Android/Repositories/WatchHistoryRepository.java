package com.example.Android.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Android.Models.Movie;
import com.example.Android.Models.User;
import com.example.Android.userinteraction.entity.WatchHistory;

/**
 * Repository thao tác bảng watch_history phục vụ gợi ý.
 */
@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    /**
     * Lấy lịch sử xem theo user, mới nhất trước.
     */
    List<WatchHistory> findByUserOrderByWatchedAtDesc(User user);

    /**
     * Kiểm tra phim đã có trong lịch sử xem hay chưa.
     */
    boolean existsByUserAndMovie(User user, Movie movie);

    /**
     * Xóa lịch sử xem của user (khi xóa tài khoản).
     */
    void deleteByUser(User user);
}
