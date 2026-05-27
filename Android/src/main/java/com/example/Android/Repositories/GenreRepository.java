package com.example.Android.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Android.Models.Genre;

/**
 * Repository truy xuất thể loại, dùng khi dựng danh sách gợi ý và preference.
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    /**
     * Tìm thể loại theo tên.
     */
    Optional<Genre> findByName(String name);

    /**
     * Kiểm tra thể loại đã tồn tại theo tên hay chưa.
     */
    boolean existsByName(String name);
}
