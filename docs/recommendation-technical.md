# Tài liệu kỹ thuật - Chức năng Gợi ý phim

## 1. Mô tả chức năng

Tạo danh sách phim đề xuất cá nhân hóa cho từng người dùng dựa trên 2 chiến lược:

| Ưu tiên | Chiến lược | Điều kiện | Mô tả |
|---------|-----------|-----------|-------|
| 1 | `INTERACTION_BASED` | Có tương tác | Điểm từ hành vi (xem=2đ, đặt vé=5đ) → gợi ý phim cùng thể loại |
| 2 | `GENRE_FALLBACK` | Không có tương tác | Gợi ý dựa trên thể loại đã chọn |

Hỗ trợ 2 chế độ: **đồng bộ** (API trả ngay) và **bất đồng bộ** (qua RabbitMQ, lưu snapshot vào DB).

---

## 2. Danh sách chức năng

| STT | Chức năng | API / Sự kiện | Mô tả |
|-----|-----------|---------------|-------|
| 1 | Lấy thể loại | `GET /api/recommendations/genres` | Danh sách thể loại cho user chọn |
| 2 | Lưu thể loại | `POST /api/recommendations/select/genres` | Lưu + publish event RabbitMQ |
| 3 | Ghi nhận xem phim | `POST /api/movies/select` | Lưu watch_history + interaction (2đ) |
| 4 | Ghi nhận đặt vé | Gọi nội bộ từ PaymentService | Interaction BOOKING_CONFIRMED (5đ) |
| 5 | Lấy gợi ý (đồng bộ) | `GET /api/recommendations/movies?limit=10` | Tính và trả ngay |
| 6 | Lưu snapshot (bất đồng bộ) | Consumer nghe `recommendationQueue` | Tính + lưu vào movie_recommendations |

---

## 3. Kiến trúc

```
Client (RecommendScreen / PreferenceGenres)
  │ REST API
  ▼
RecommendationController ──▶ RecommendationService (logic chính)
  │                              │
  │                              ├──▶ UserMovieInteractionRepository
  │                              ├──▶ MovieGenreRepository
  │                              ├──▶ UserGenrePreferenceRepository
  │                              └──▶ MovieRecommendationRepository
  │
  └──▶ RecommendationProducer ──▶ RabbitMQ ──▶ RecommendationConsumer
         (publish events)       Exchange         (consume → processRecommendationEvent)
```

### Luồng 1: Đồng bộ

GET `/api/recommendations/movies?limit=10` → Lấy interactions → Có? → INTERACTION_BASED (trọng số thể loại = tổng điểm tương tác) → Không? → GENRE_FALLBACK (theo thể loại đã chọn) → Trả danh sách.

### Luồng 2: Bất đồng bộ

POST `/api/recommendations/select/genres` → Lưu user_genre_preferences → Producer.sendMessage() → RabbitMQ → Consumer.receiveMessage() → processRecommendationEvent() → Xóa snapshot cũ → Lưu snapshot mới vào movie_recommendations.

### Luồng 3: Ghi nhận đặt vé

PaymentService (khi thanh toán thành công) → `saveBookingInteraction(booking)` → Tạo interaction type="BOOKING_CONFIRMED", score=5.

---

## 4. Code

### 4.1 Backend - Các lớp chính

#### `RecommendationController.java` — `Controllers/`

| Hàm | Endpoint | Auth | Mô tả |
|-----|----------|------|-------|
| `getGenres()` | `GET /api/recommendations/genres` | Không | Lấy toàn bộ thể loại |
| `postMethodName()` | `POST /api/recommendations/select/genres` | JWT | Lưu thể loại + publish RabbitMQ, trả 202 |
| `getRecommendations()` | `GET /api/recommendations/movies?limit=10` | JWT | Tính gợi ý đồng bộ |

#### `RecommendationService.java` — `Services/`

Đây là **lớp xử lý nghiệp vụ chính**.

| Hàm | Mô tả |
|-----|-------|
| `recommendMovies(principal, limit)` | API đồng bộ: resolve user → lấy interactions → Có → `recommendFromInteractions()` / Không → `recommendFromSelectedGenres()` |
| `processRecommendationEvent(principal, limit)` | Xử lý bất đồng bộ: tính gợi ý → xóa snapshot cũ → lưu snapshot mới |
| `recommendFromInteractions(interactions, limit)` | **INTERACTION_BASED**: (1) Lấy movie đã tương tác. (2) Trọng số thể loại = tổng điểm (ví dụ phim Action+Comedy, score=5 → Action+=5, Comedy+=5). (3) Tìm phim ứng viên (cùng thể loại, chưa tương tác). (4) Điểm ứng viên = tổng trọng số khớp. (5) Sắp xếp giảm dần, lấy top N |
| `recommendFromSelectedGenres(user, limit)` | **GENRE_FALLBACK**: (1) Lấy thể loại đã chọn. (2) Tìm phim trùng thể loại, +1 điểm mỗi match. (3) Sắp xếp, lấy top N |
| `buildRecommendationResult(scoreMap, strategy, limit)` | Sắp xếp Map<movieId, score> → lấy top N → join movies để lấy title, posterUrl, status → trả List<RecommendedMovieItem> |
| `saveSelectedGenres(principal, genreIds)` | So sánh thể loại mới/cũ → xóa cũ không còn chọn → thêm mới → lưu user_genre_preferences |
| `saveWatchHistory(principal, movieId)` | Lưu watch_history + tạo interaction MOVIE_SELECTED (2đ) |
| `saveBookingInteraction(booking)` | Tạo interaction BOOKING_CONFIRMED (5đ) |

#### `RecommendationProducer.java` — `Services/`

| Hàm | Mô tả |
|-----|-------|
| `sendMessage(genreIds, userId)` | Publish `{eventType: "GENRES_SELECTED", genreIds, userId, limit: 20}` → `recommendationExchange` / `recommendationRoutingKey` |
| `sendMovieSelectedEvent(movieId, userId)` | Publish `{eventType: "MOVIE_SELECTED", movieId, userId, limit: 20}` |

#### `RecommendationConsumer.java` — `Services/`

`@RabbitListener(queues = "recommendationQueue")` → Parse message → Gọi `processRecommendationEvent(userId, limit)`.

#### `RabbitConfig.java` — `Configs/`

| Bean | Tên | Loại |
|------|-----|------|
| `queue()` | `recommendationQueue` | Queue (durable=false) |
| `exchange()` | `recommendationExchange` | DirectExchange |
| `binding()` | - | Bind queue → exchange, routing key `recommendationRoutingKey` |

### 4.2 Backend - Các lớp phụ

| Lớp | Vai trò |
|-----|---------|
| `MovieRecommendationRepository` | `findByUserOrderByScoreDesc()`, `deleteByUser()` |
| `UserMovieInteractionRepository` | `findByUserOrderByCreatedAtDesc()`, `deleteByUser()` |
| `WatchHistoryRepository` | `existsByUserAndMovie()`, `findByUserOrderByWatchedAtDesc()` |
| `UserGenrePreferenceRepository` | CRUD bảng user_genre_preferences |
| `MovieGenreRepository` | CRUD bảng movie_genres (many-to-many) |
| `RecommendationMoviesResponse` | DTO: extends BaseResponse, thêm `movies: List<RecommendedMovieItem>` |
| `RecommendedMovieItem` | DTO: `{movieId, title, posterUrl, status, score, strategy}` |
| `GenresSelectReq` | DTO: `{genres: List<Genre>}` |

### 4.3 Frontend

#### `RecommendScreen.js`

- Gọi `getRecommendedMovies(10)` → lấy chi tiết từng phim qua `getMovieById()`
- Hiển thị featured movie (lớn) + danh sách gợi ý bên dưới
- Genre chips filter → gọi `selectRecommendationGenres()` → reload gợi ý
- Match %: `max(70, min(99, score * 100))`
- Tap phim → `selectMovieForRecommendation(movieId)` (fire-and-forget) → navigate MovieDetail

#### `PreferenceGenres.js`

- Gọi `getRecommendationGenres()` → lưới thể loại (2 cột)
- Multi-select → `selectRecommendationGenres(selectedIds)` → quay lại

#### `movieService.js` (phần recommendation)

| Hàm | API |
|-----|-----|
| `getRecommendationGenres()` | `GET /api/recommendations/genres` |
| `selectRecommendationGenres(genreIds)` | `POST /api/recommendations/select/genres` |
| `getRecommendedMovies(limit)` | `GET /api/recommendations/movies?limit=N` |
| `selectMovieForRecommendation(movieId)` | `POST /api/movies/select` |

---

### 4.4 Bảng CSDL

| Bảng | Mô tả | Các cột chính |
|------|-------|---------------|
| `movie_recommendations` | Snapshot gợi ý (bất đồng bộ) | `id`, `user_id` (FK), `movie_id` (FK), `score` (DECIMAL) |
| `user_movie_interactions` | Hành vi tương tác | `id`, `user_id`, `movie_id`, `type` (VARCHAR), `score` (INT), `created_at` |
| `watch_history` | Lịch sử xem | `id`, `user_id`, `movie_id`, `watched_at` |
| `user_genre_preferences` | Thể loại yêu thích | `id`, `user_id`, `genre_id`, `created_at`. UNIQUE(user_id, genre_id) |
| `movie_genres` | Phim-Thể loại (many-to-many) | `movie_id` (FK), `genre_id` (FK). UNIQUE(movie_id, genre_id) |

**Điểm tương tác**: `MOVIE_SELECTED` = 2 điểm, `BOOKING_CONFIRMED` = 5 điểm.

---

### 4.5 API Endpoints

| Method | Endpoint | Auth | Request | Response |
|--------|----------|------|---------|----------|
| GET | `/api/recommendations/genres` | Không | - | `{statusCode, message, genres: [{id, name}]}` |
| POST | `/api/recommendations/select/genres` | JWT | `{genres: [{id:1}, {id:3}]}` | `"Genres Accept"` (202) |
| GET | `/api/recommendations/movies?limit=10` | JWT | - | `{statusCode, message, movies: [{movieId, title, posterUrl, status, score, strategy}]}` |

---

## 5. Lưu ý triển khai

- RabbitMQ không sẵn sàng → Producer bắt exception, log lỗi. API đồng bộ vẫn hoạt động bình thường.
- Queue `recommendationQueue` durable=false → mất khi restart. Đổi thành `true` nếu cần bền vững.
- Thuật toán tính trực tiếp trên DB mỗi lần gọi (không cache). Nên thêm Redis cho dữ liệu lớn.
- `recommendFromInteractions()` load toàn bộ interactions vào memory → cần phân trang nếu user có lịch sử lớn.
- Cần dữ liệu seed trong `genres` và `movie_genres` trước khi gợi ý hoạt động.
- Mobile: `RecommendScreen` gọi `getMovieById()` cho từng phim gợi ý → N+1 API calls. Nên tối ưu backend trả thêm thông tin.
