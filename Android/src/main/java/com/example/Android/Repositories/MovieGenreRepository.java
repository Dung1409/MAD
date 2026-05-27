package com.example.Android.Repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Android.Models.MovieGenre;

/**
 * Repository truy vấn mapping movie - genre để tính điểm gợi ý.
 */
@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, Long> {
    /**
     * Lấy mapping theo danh sách movieId.
     */
    List<MovieGenre> findByMovie_IdIn(Collection<Long> movieIds);

    /**
     * Lấy mapping theo danh sách genreId.
     */
    List<MovieGenre> findByGenre_IdIn(Collection<Long> genreIds);
}
