package com.example.Android.DTO.Requests;

import java.util.List;
import com.example.Android.Models.Genre;

import lombok.Builder;
import lombok.Data;

/**
 * DTO request gửi danh sách thể loại người dùng chọn cho gợi ý.
 */
@Builder
@Data
public class GenresSelectReq {
    /**
     * Danh sách genre (chỉ cần id) được client gửi.
     */
    List<Genre> genres;
}
