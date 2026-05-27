# Kiến trúc chức năng Gợi ý phim (Movie Recommendation)

## 1. Mô tả tổng quan

Hệ thống gợi ý phim sử dụng chiến lược **content-based filtering**, hoạt động ở 2 chế độ:

- **Đồng bộ**: FE gọi API `GET /api/recommendations/movies`, BE tính toán và trả kết quả trực tiếp.
- **Bất đồng bộ**: Khi người dùng chọn thể loại hoặc xem phim, sự kiện được gửi qua RabbitMQ -> Consumer xử lý và lưu snapshot vào bảng `movie_recommendations`.

Hệ thống có 3 chiến lược gợi ý theo thứ tự ưu tiên:

| Ưu tiên | Chiến lược | Điều kiện | Trọng số |
|---------|-----------|-----------|----------|
| 1 | `INTERACTION_BASED` | Có bản ghi `user_movie_interactions` | Điểm tương tác (xem=2, đặt vé=5) |
| 2 | `CONTENT_BASED_HISTORY` | Có `watch_history` nhưng chưa có interaction | Mỗi phim đã xem = +1/genre |
| 3 | `GENRE_FALLBACK` | Không có history/interaction | Thể loại người dùng đã chọn |

---

## 2. Luồng dữ liệu chi tiết

### Flow 1: Người dùng chọn thể loại yêu thích

```
PreferenceGenres.js
    │  POST /api/recommendations/select/genres
    │  Body: { genres: [{id: 1}, {id: 3}] }
    ▼
RecommendationController.selectGenres()
    │
    ├──► RecommendationService.saveSelectedGenres()
    │       └── Lưu vào bảng user_genre_preferences (xóa cũ, thêm mới)
    │
    └──► RecommendationProducer.sendMessage(genreIds, userId)
            │  Gửi message đến RabbitMQ:
            │  { eventType: "GENRES_SELECTED", genreIds, userId, limit: 20 }
            ▼
        RecommendationConsumer.receiveMessage()
            │
            └──► RecommendationService.processRecommendationEvent()
                    ├── Tính toán gợi ý (theo priority cascade)
                    ├── Xóa snapshot cũ (deleteByUser)
                    └── Lưu snapshot mới vào movie_recommendations
```

### Flow 2: Người dùng xem/chọn phim

```
HomeScreen.js / SearchScreen.js / RecommendScreen.js
    │  POST /api/movies/select  { movieId: N }
    ▼
MovieController.selectMovie()
    │
    ├──► RecommendationService.saveWatchHistory()
    │       ├── Lưu watch_history (nếu mới)
    │       └── Lưu user_movie_interactions (type="MOVIE_SELECTED", score=2)
    │
    └──► RecommendationProducer.sendMovieSelectedEvent(movieId, userId)
            │  Gửi message đến RabbitMQ
            ▼
        RecommendationConsumer → processRecommendationEvent()
            └── Tính lại và lưu snapshot mới
```

### Flow 3: Người dùng đặt vé thành công

```
PaymentService (VNPAY IPN / Return / Manual confirm)
    │
    └──► RecommendationService.saveBookingInteraction(booking)
            └── Lưu user_movie_interactions (type="BOOKING_CONFIRMED", score=5)
```

### Flow 4: Xem màn hình gợi ý

```
RecommendScreen.js
    │  GET /api/recommendations/movies?limit=10
    ▼
RecommendationController.getRecommendations()
    │
    └──► RecommendationService.recommendMovies(userId, 10)
            │  Priority: interactions > watch history > genre preferences
            └── Trả về RecommendationMoviesResponse { movies: [...] }
    │
    ▼
RecommendScreen (FE)
    ├── Chuẩn hóa response (normalizeRecommendations)
    ├── Gọi GET /api/movies/{id} cho mỗi item để lấy chi tiết đầy đủ
    ├── Hiển thị featured movie (item đầu tiên) dạng hero card
    └── Hiển thị danh sách còn lại dạng card list
```

---

## 3. Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────┐
│                  React Native (FE)                   │
│                                                       │
│  PreferenceGenres ──► movieService.selectRecommendationGenres()
│  RecommendScreen  ──► movieService.getRecommendedMovies()
│                     ──► movieService.selectMovieForRecommendation()
│  HomeScreen/Search ──► movieService.selectMovieForRecommendation()
└──────────────┬──────────────────────┬────────────────┘
               │ HTTP (JWT)           │ HTTP (JWT)
               ▼                      ▼
┌──────────────────────────────────────────────────────┐
│              Spring Boot (BE)                         │
│                                                       │
│  RecommendationController    MovieController          │
│    /genres                     /select                │
│    /select/genres                                      │
│    /movies                                             │
│         │                          │                  │
│         ▼                          ▼                  │
│  RecommendationService (core logic)                   │
│    ├── recommendMovies() [sync]                       │
│    ├── processRecommendationEvent() [async]           │
│    ├── saveSelectedGenres()                           │
│    ├── saveWatchHistory()                             │
│    └── saveBookingInteraction()                       │
│         │                                             │
│    ┌────┴────┐                                        │
│    ▼         ▼                                        │
│  Producer  Consumer                                   │
│    │         ▲                                        │
└────┼─────────┼────────────────────────────────────────┘
     │         │
     ▼         │
┌─────────────────┐
│    RabbitMQ      │
│  recommendation  │
│  Exchange/Queue  │
│  (non-durable)   │
└─────────────────┘
```

---

## 4. Cấu trúc code

### Frontend (React Native)

| File | Vai trò |
|------|---------|
| `app/services/movieService.js` | 4 method recommendation: `getRecommendationGenres`, `selectRecommendationGenres`, `getRecommendedMovies`, `selectMovieForRecommendation` |
| `app/screens/user/PreferenceGenres.js` | Màn hình chọn thể loại (multi-select grid, lưu preference) |
| `app/screens/user/RecommendScreen.js` | Màn hình hiển thị gợi ý (hero card + list, genre filter chips, pull-to-refresh) |
| `app/screens/user/HomeScreen.js` | Gọi `selectMovieForRecommendation` khi user nhấn phim |
| `app/screens/user/SearchScreen.js` | Gọi `selectMovieForRecommendation` khi user nhấn phim |
| `app/navigation/UserTabNavigator.js` | Đăng ký tab "Suggestions" với RecommendScreen |

### Backend - Controllers

| File | Endpoints |
|------|-----------|
| `RecommendationController.java` | `GET /genres`, `POST /select/genres`, `GET /movies` |
| `MovieController.java` | `POST /select` (ghi nhận tương tác phim) |

### Backend - Services

| File | Vai trò |
|------|---------|
| `RecommendationService.java` | Core logic: tính toán gợi ý, lưu preference/history/interaction |
| `RecommendationProducer.java` | Gửi message RabbitMQ (`GENRES_SELECTED`, `MOVIE_SELECTED`) |
| `RecommendationConsumer.java` | Lắng nghe queue `recommendationQueue`, gọi `processRecommendationEvent()` |
| `PaymentService.java` | Gọi `saveBookingInteraction()` khi thanh toán thành công (score=5) |

### Backend - Entities (4 bảng chính)

| Entity | Bảng | Mô tả |
|--------|------|-------|
| `UserGenrePreference` | `user_genre_preferences` | Thể loại yêu thích (UNIQUE user_id + genre_id) |
| `WatchHistory` | `watch_history` | Lịch sử xem phim |
| `UserMovieInteraction` | `user_movie_interactions` | Tương tác: type + score (xem=2, đặt vé=5) |
| `MovieRecommendation` | `movie_recommendations` | Snapshot gợi ý (chỉ dùng cho async flow) |

### Backend - DTOs

| DTO | Direction | Fields |
|-----|-----------|--------|
| `GenresSelectReq` | Request | `genres: List<Genre>` (chỉ cần id) |
| `MovieSelectReq` | Request | `movieId: Long` |
| `RecommendationMoviesResponse` | Response | extends BaseResponse + `movies: List<RecommendedMovieItem>` |
| `RecommendedMovieItem` | Response | `movieId, title, posterUrl, status, score, strategy` |

### Backend - RabbitMQ Config

| Bean | Tên | Ghi chú |
|------|-----|---------|
| Queue | `recommendationQueue` | durable=false |
| Exchange | `recommendationExchange` | DirectExchange |
| Routing key | `recommendationRoutingKey` | Binding queue ↔ exchange |

---

## 5. Triển khai & Yêu cầu

### Điều kiện dữ liệu

- Bảng `genres` phải có dữ liệu (seed data).
- Bảng `movie_genres` phải có dữ liệu (liên kết phim - thể loại).
- RabbitMQ phải chạy trên localhost:5672 (default). Nếu không, Producer sẽ catch exception và log lỗi, API vẫn hoạt động bình thường nhưng async flow sẽ mất.

### Dependencies

- Spring Boot: `spring-boot-starter-amqp` (RabbitMQ)
- Database: MySQL với JPA/Hibernate
- FE: Axios (qua `apiClient.js`), AsyncStorage cho JWT token

### API endpoints tóm tắt

```
GET  /api/recommendations/genres           → Lấy danh sách thể loại
POST /api/recommendations/select/genres    → Lưu preference + trigger async
GET  /api/recommendations/movies?limit=N   → Lấy gợi ý (sync)
POST /api/movies/select                    → Ghi nhận tương tác xem phim
```
