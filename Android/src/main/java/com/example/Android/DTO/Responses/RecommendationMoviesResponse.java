package com.example.Android.DTO.Responses;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * DTO response cho danh sách phim gợi ý.
 */
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class RecommendationMoviesResponse extends BaseResponse {
    List<RecommendedMovieItem> movies;
}
