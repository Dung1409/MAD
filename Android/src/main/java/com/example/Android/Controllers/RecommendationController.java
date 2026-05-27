package com.example.Android.Controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Android.DTO.Requests.GenresSelectReq;
import com.example.Android.Models.Genre;
import com.example.Android.Services.RecommendationProducer;
import com.example.Android.Services.RecommendationService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Controller gợi ý phim: cung cấp API lấy thể loại, lưu preference và trả danh
 * sách gợi ý.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationController {
    RecommendationService recommendationService;
    RecommendationProducer recommendationProducer;

    /**
     * Lấy danh sách thể loại để người dùng chọn cho gợi ý.
     * 
     * @return response chứa danh sách genre.
     */
    @GetMapping("/genres")
    public ResponseEntity<?> getGenres() {
        return recommendationService.getAllGenres();
    }

    /**
     * Lưu danh sách thể loại đã chọn và phát sự kiện gợi ý bất đồng bộ.
     * 
     * @param req danh sách genre (id) do client gửi.
     * @return HTTP 202 nếu tiếp nhận, hoặc lỗi khi dữ liệu không hợp lệ.
     */
    @PostMapping("/select/genres")
    public ResponseEntity<?> selectGenres(@RequestBody GenresSelectReq req) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Selected genres for user: {}", authentication.getName());
        String userId = authentication.getName();
        List<Genre> genres = req.getGenres();
        if (genres == null || genres.isEmpty()) {
            return ResponseEntity.badRequest().body("Genres list cannot be empty");
        }

        List<Long> genreIds = genres.stream()
                .map(Genre::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        ResponseEntity<String> saveResult = recommendationService.saveSelectedGenres(userId, genreIds);
        if (!saveResult.getStatusCode().is2xxSuccessful()) {
            return saveResult;
        }

        recommendationProducer.sendMessage(genreIds, userId);
        return ResponseEntity.status(202).body("Genres Accept");
    }

    /**
     * Trả danh sách phim gợi ý theo hạn mức.
     * 
     * @param limit số lượng phim gợi ý tối đa.
     * @return response chứa danh sách gợi ý (score/strategy kèm theo).
     */
    @GetMapping("/movies")
    public ResponseEntity<?> getRecommendations(@RequestParam(defaultValue = "10") Integer limit) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        return recommendationService.recommendMovies(userId, limit);
    }

}
