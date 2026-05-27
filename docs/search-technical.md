# Tài liệu kỹ thuật - Chức năng Tìm kiếm phim

## 1. Mô tả chức năng

Cho phép người dùng tìm phim theo **từ khóa** (title, description), lọc theo **thể loại** và **đánh giá**. Tìm kiếm không phân biệt hoa thường. Bộ lọc rating áp dụng ở client sau khi nhận kết quả từ API.

---

## 2. Danh sách chức năng

| STT | Chức năng | API | Mô tả |
|-----|-----------|-----|-------|
| 1 | Tìm theo từ khóa | `GET /api/movies/search?keyword=abc` | Tìm trong title và description |
| 2 | Lọc theo thể loại | `GET /api/movies/genre/{genre}` | JOIN với bảng movie_genres |
| 3 | Lấy tất cả phim | `GET /api/movies` | Danh sách đầy đủ |
| 4 | Phim nổi bật | `GET /api/movies/featured` | Rating >= 8.0 |
| 5 | Chi tiết phim | `GET /api/movies/{id}` | Thông tin chi tiết |
| 6 | Ghi nhận xem phim | `POST /api/movies/select` | Lưu lịch sử + tương tác (JWT) |

---

## 3. Kiến trúc

```
Client (SearchScreen/MovieDetailScreen) ──▶ movieService.js
       │ HTTP REST                               │
       ▼                                         ▼
MovieController ──▶ MovieService ──▶ MovieRepository (MySQL)
                      │                 └── findByKeyword() (JPQL)
                      │                 └── findByGenreName() (JPQL)
                      └──▶ RecommendationService (ghi nhận xem phim)
```

### Luồng xử lý

**Tìm kiếm**: TextInput debounce 500ms → GET `/api/movies/search?keyword=` → `MovieRepository.findByKeyword()` → JPQL `WHERE LOWER(title) LIKE '%kw%' OR LOWER(description) LIKE '%kw%'` → Client filter theo rating/genre.

**Lọc thể loại**: Chip chọn genre → GET `/api/movies/genre/Action` → `findByGenreName()` → JPQL `JOIN movie_genres WHERE LOWER(genre.name) = :genre`.

**Xem chi tiết**: Tap phim → `selectMovieForRecommendation(movieId)` (fire-and-forget, ghi nhận tương tác) → navigate MovieDetailScreen → GET `/api/movies/{id}`.

---

## 4. Code

### 4.1 Backend - Các lớp chính

#### `MovieController.java` — `Controllers/`

| Hàm | Endpoint | Auth | Mô tả |
|-----|----------|------|-------|
| `getAllMovies()` | `GET /api/movies` | Không | Trả `MoviesListResponse` |
| `getMovieById()` | `GET /api/movies/{id}` | Không | Trả `MovieResponse` hoặc 404 |
| `searchMovies()` | `GET /api/movies/search?keyword=` | Không | Keyword rỗng → 400 |
| `getMoviesByGenre()` | `GET /api/movies/genre/{genre}` | Không | JOIN movie_genres |
| `getFeaturedMovies()` | `GET /api/movies/featured` | Không | rating >= 8.0 |
| `selectMovie()` | `POST /api/movies/select` | JWT | Lưu lịch sử xem + publish RabbitMQ event |

#### `MovieService.java` — `Services/`

| Hàm | Mô tả |
|-----|-------|
| `searchMovies(keyword)` | `movieRepository.findByKeyword(keyword)` → convert sang MovieResponse |
| `getMoviesByGenre(genre)` | `movieRepository.findByGenreName(genre)` → convert |
| `getAllMovies()` | `findAllByOrderByIdDesc()` → convert |
| `getMovieById(id)` | `findById()` → convert |
| `getHighRatedMovies(minRating)` | `findByRatingGreaterThanEqual()` → convert |
| `convertToMovieResponse(movie)` | Movie → MovieResponse, lấy genres list từ quan hệ ManyToMany |

#### `MovieRepository.java` — `Repositories/`

| Hàm | Loại | Mô tả |
|-----|------|-------|
| `findByKeyword(keyword)` | JPQL | `WHERE LOWER(title) LIKE '%kw%' OR LOWER(description) LIKE '%kw%'` |
| `findByGenreName(genreName)` | JPQL | `SELECT DISTINCT m FROM Movie m JOIN m.genres g WHERE LOWER(g.name) = :name` |
| `findByRatingGreaterThanEqual(minRating)` | JPQL | `WHERE rating >= :minRating ORDER BY rating DESC` |
| `findAllByOrderByIdDesc()` | Method query | Tất cả phim, ID giảm dần |
| `findByStatus(status)` | Method query | Lọc theo trạng thái |

### 4.2 Backend - Các lớp phụ

| Lớp | Vai trò |
|-----|---------|
| `Movie` | Entity: `id, title, description, genre(String), genres(ManyToMany→Genre), duration, rating, posterUrl, backdropUrl, trailerUrl, releaseDate, status` |
| `Genre` | Entity: `id, name(unique)`. Quan hệ ManyToMany với Movie qua `movie_genres` |
| `MovieResponse` | DTO: `{id, title, description, genre, genres[], duration, rating, posterUrl, backdropUrl, trailerUrl, releaseDate, status, createdAt}` |
| `MoviesListResponse` | DTO: `{movies: List<MovieResponse>, totalCount, message}` |
| `MovieSelectReq` | DTO request: `{movieId: Long}` |

### 4.3 Frontend

#### `SearchScreen.js`

- Tải tất cả phim khi mở màn hình (`loadAllMovies()`)
- **Debounce 500ms** trên search input → gọi `performSearch()`
- `performSearch()`: keyword → `searchMovies()` / genre → `getMoviesByGenre()` / không → `getAllMovies()`. Sau đó local filter theo rating + genre.
- Genre chips: horizontal scroll, "All" + danh sách từ API
- Rating chips: All / 7+ / 8+ / 9+
- FlatList 2 cột, mỗi card: poster + title + genre + rating + duration
- `openMovieDetail(movieId)`: fire-and-forget `selectMovieForRecommendation()` → navigate

#### `MovieDetailScreen.js`

Gọi `getMovieById(movieId)` → hiển thị backdrop, title, genres, duration, rating, description, nút "Book Ticket" → navigate ShowtimeScreen, nút trailer.

#### `movieService.js` (phần search)

| Hàm | API |
|-----|-----|
| `getAllMovies()` | `GET /api/movies` |
| `getMovieById(id)` | `GET /api/movies/{id}` |
| `searchMovies(keyword)` | `GET /api/movies/search?keyword=` |
| `getMoviesByGenre(genre)` | `GET /api/movies/genre/{genre}` |
| `getFeaturedMovies()` | `GET /api/movies/featured` |
| `selectMovieForRecommendation(movieId)` | `POST /api/movies/select` |

---

### 4.4 Bảng CSDL

#### `movies`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT, PK | Khóa chính |
| `title` | VARCHAR, NOT NULL | Tên phim |
| `description` | TEXT | Mô tả |
| `genre` | VARCHAR | Thể loại dạng text (legacy) |
| `duration` | INT | Thời lượng (phút) |
| `rating` | DOUBLE | Điểm đánh giá |
| `poster_url` / `backdrop_url` / `trailer_url` | VARCHAR | URLs |
| `release_date` | DATETIME | Ngày phát hành |
| `status` | VARCHAR | "NOW_SHOWING", "COMING_SOON" |
| `created_at` / `updated_at` | DATETIME | Timestamps |

Quan hệ: `@ManyToMany` với `Genre` qua `movie_genres`.

#### `movie_genres` (bảng trung gian)

`movie_id` (FK) + `genre_id` (FK), UNIQUE(movie_id, genre_id)

#### `genres`

`id` (PK) + `name` (VARCHAR, UNIQUE, NOT NULL)

---

## 5. Lưu ý triển khai

- `findByKeyword()` dùng `LIKE '%keyword%'` → full table scan. Dữ liệu lớn cần full-text index.
- Field `genre` (VARCHAR) trên Movie là legacy, khác với `genres` (ManyToMany). Cần đồng bộ cả 2 khi tạo/sửa phim.
- Rating filter + genre filter khi kết hợp keyword search: áp dụng ở **client** (không phải server-side combined query).
- `convertToMovieResponse()` truy cập `movie.getGenres()` (lazy loading) → có thể gây N+1 queries.
- `selectMovieForRecommendation()` gọi fire-and-forget, không đợi kết quả.
