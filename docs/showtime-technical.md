# Tài liệu kỹ thuật - Chức năng Xem xuất chiếu

## 1. Mô tả chức năng

Cho phép người dùng xem danh sách suất chiếu theo **phim**, **rạp** và **ngày**. Chọn ngày → xem rạp → chọn giờ → chuyển sang chọn ghế. Hỗ trợ lọc kết hợp 3 tham số `movieId`, `cinemaId`, `date`. Khi tạo suất chiếu mới, tự động tạo ShowtimeSeat cho tất cả ghế trong phòng.

---

## 2. Danh sách chức năng

| STT | Chức năng | API | Mô tả |
|-----|-----------|-----|-------|
| 1 | Lấy danh sách xuất chiếu | `GET /api/showtimes?movieId=&cinemaId=&date=` | Lọc theo phim, rạp, ngày |
| 2 | Lấy chi tiết | `GET /api/showtimes/{id}` | Thông tin 1 suất chiếu |
| 3 | Tạo mới (admin) | `POST /api/showtimes` | Tạo suất chiếu + auto bootstrap seats |
| 4 | Cập nhật (admin) | `PUT /api/showtimes/{id}` | Cập nhật giờ, giá, phòng |
| 5 | Xóa (admin) | `DELETE /api/showtimes/{id}` | Xóa suất chiếu + ghế liên quan |

---

## 3. Kiến trúc

```
Client (ShowtimeScreen)
  │ GET /api/showtimes?movieId=1&date=2026-05-26
  ▼
ShowtimeController ──▶ ShowtimeService ──▶ ShowtimeRepository (MySQL)
                         │                    └── findByMovieIdAndDateRange() (JPQL)
                         ├──▶ SeatRepository (lấy ghế theo phòng)
                         └──▶ ShowtimeSeatRepository (tạo/xóa ghế suất chiếu)
```

### Luồng xử lý

**Xem xuất chiếu**: MovieDetail → "Book Ticket" → ShowtimeScreen → Tạo 7 ngày (hôm nay + 6 ngày) → Chọn ngày → GET `/api/showtimes?movieId=&date=` → **Nhóm theo cinema** (lấy từ `showtime.room.cinema`) → Hiển thị danh sách rạp + giờ chiếu → Chọn giờ → "Select Seats" → navigate SeatSelection.

**Logic kết hợp tham số trong Controller**:

| movieId | cinemaId | date | Hàm gọi |
|---------|----------|------|---------|
| Có | null | Có | `getShowtimesByMovieAndDate()` |
| Có | null | null | `getShowtimesByMovie()` |
| null | Có | Có | `getShowtimesByCinemaAndDate()` |
| null | Có | null | `getShowtimesByCinema()` |
| Có | Có | Có | `getShowtimesByMovieAndCinemaAndDate()` |
| null | null | null | `getAllShowtimes()` |

**Tạo suất chiếu (admin)**: Validate startTime < endTime → Save → `bootstrapShowtimeSeats()`: lấy ghế trong phòng → tạo ShowtimeSeat cho mỗi ghế (price=basePrice, status="available").

**Cập nhật + đổi phòng**: Detect `roomChanged` → xóa ghế cũ → bootstrap ghế mới cho phòng mới.

---

## 4. Code

### 4.1 Backend - Các lớp chính

#### `ShowtimeController.java` — `Controllers/`

| Hàm | Endpoint | Auth | Mô tả |
|-----|----------|------|-------|
| `getShowtimes()` | `GET /api/showtimes?movieId=&cinemaId=&date=` | Không | Logic kết hợp 7 trường hợp theo null/not-null |
| `getShowtimeById()` | `GET /api/showtimes/{id}` | Không | Trả 1 Showtime |
| `createShowtime()` | `POST /api/showtimes` | Không | Tạo mới + bootstrap seats |
| `updateShowtime()` | `PUT /api/showtimes/{id}` | Không | Cập nhật + xử lý đổi phòng |
| `deleteShowtime()` | `DELETE /api/showtimes/{id}` | Không | Xóa + xóa seats |

#### `ShowtimeService.java` — `Services/`

| Hàm | Mô tả |
|-----|-------|
| `getShowtimesByMovieAndDate(movieId, date)` | Convert LocalDate → startOfDay/nextDay → `findByMovieIdAndDateRange()` |
| `getShowtimesByMovie(movieId)` | `findByMovieId(movieId)` |
| `getShowtimesByCinema(cinemaId)` | `findByRoomCinemaId(cinemaId)` |
| `getShowtimesByMovieAndCinema(movieId, cinemaId)` | `findByMovieIdAndCinemaId()` |
| `getShowtimesByMovieAndCinemaAndDate(movieId, cinemaId, date)` | `findByMovieIdAndCinemaIdAndDateRange()` |
| `getShowtimesByCinemaAndDate(cinemaId, date)` | `findByCinemaIdAndDateRange()` |
| `createShowtime(showtime)` | Validate → save → `bootstrapShowtimeSeats()` |
| `updateShowtime(id, details)` | Validate → update → save → nếu đổi phòng → xóa ghế cũ + bootstrap mới |
| `deleteShowtime(id)` | Xóa seats → xóa showtime |
| `bootstrapShowtimeSeats(showtime)` | **Quan trọng**: Kiểm tra chưa có seats → `seatRepository.findByRoomId()` → tạo ShowtimeSeat cho mỗi ghế (price=basePrice, status="available") → `saveAll()` |

#### `ShowtimeRepository.java` — `Repositories/`

| Hàm | Mô tả |
|-----|-------|
| `findByMovieIdAndDateRange(movieId, start, end)` | JPQL: `WHERE movie.id=:id AND startTime >= :start AND startTime < :end ORDER BY startTime ASC` |
| `findByMovieIdAndCinemaId(movieId, cinemaId)` | JPQL: kết hợp movie + cinema |
| `findByMovieIdAndCinemaIdAndDateRange(movieId, cinemaId, start, end)` | JPQL: kết hợp cả 3 |
| `findByRoomCinemaId(cinemaId)` | Method query: qua room → cinema |
| `findByCinemaIdAndDateRange(cinemaId, start, end)` | JPQL: cinema + date range |

### 4.2 Backend - Các lớp phụ

| Lớp | Vai trò |
|-----|---------|
| `Showtime` | Entity: `id, movie(FK), room(FK), startTime, endTime, basePrice`. @OneToMany → ShowtimeSeat, Booking (@JsonIgnore) |
| `ShowtimeSeat` | Entity: `id, showtime(FK), seat(FK), price, status` |
| `Room` | Entity: phòng chiếu, thuộc 1 Cinema |
| `Seat` | Entity: ghế trong phòng |
| `Cinema` | Entity: rạp chiếu phim |

### 4.3 Frontend

#### `ShowtimeScreen.js`

- Nhận `movieId`, `movieTitle` từ navigation params
- `initializeDates()`: tạo 7 ngày (hôm nay + 6 ngày sau), format `YYYY-MM-DD`
- `loadShowtimes()`: gọi API → **nhóm theo cinema** (`showtime.room.cinema.id`) thành danh sách theaters
- `parseApiDateTimeToLocal()`: parse datetime từ API → JS Date, hỗ trợ cả format `T` và space
- UI: Date selector horizontal scroll → Theater cards (tên rạp, địa chỉ, giờ chiếu grid) → Footer "Select Seats →"
- Giờ chiếu được chọn → highlight đỏ → navigate SeatSelection

#### `showtimeService.js`

| Hàm | API |
|-----|-----|
| `getAllShowtimes(filters)` | `GET /api/showtimes?movieId=&date=&cinemaId=` |
| `getShowtimeById(id)` | `GET /api/showtimes/{id}` |
| `createShowtime(data)` | `POST /api/showtimes` |
| `updateShowtime(id, data)` | `PUT /api/showtimes/{id}` |
| `deleteShowtime(id)` | `DELETE /api/showtimes/{id}` |

---

### 4.4 Bảng CSDL

#### `showtimes`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT, PK | Khóa chính |
| `movie_id` | BIGINT, FK → movies | Phim |
| `room_id` | BIGINT, FK → rooms | Phòng chiếu |
| `start_time` | DATETIME | Bắt đầu |
| `end_time` | DATETIME | Kết thúc |
| `base_price` | DOUBLE | Giá vé (VND) |

#### `showtime_seats`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT, PK | Khóa chính |
| `showtime_id` | BIGINT, FK → showtimes | Suất chiếu |
| `seat_id` | BIGINT, FK → seats | Ghế |
| `price` | DOUBLE | Giá (mặc định = basePrice) |
| `status` | VARCHAR | "available" / "held" / "booked" |

Bảng liên quan: `rooms` (phòng chiếu), `seats` (ghế), `cinemas` (rạp).

---

### 4.5 API Endpoints

| Method | Endpoint | Params | Response | Auth |
|--------|----------|--------|----------|------|
| GET | `/api/showtimes` | `movieId`, `cinemaId`, `date` | `List<Showtime>` | Không |
| GET | `/api/showtimes/{id}` | - | `Showtime` | Không |
| POST | `/api/showtimes` | Body: Showtime JSON | Showtime (đã tạo) | Không |
| PUT | `/api/showtimes/{id}` | Body: Showtime JSON | Showtime (đã cập nhật) | Không |
| DELETE | `/api/showtimes/{id}` | - | 204 | Không |

**Lưu ý**: Controller trả trực tiếp `Showtime` entity (không dùng DTO). `showtimeSeats` và `bookings` có `@JsonIgnore`.

---

## 5. Lưu ý triển khai

- Tất cả endpoints đều `permitAll()` → cần chuyển CRUD sang `authenticated()` + role ADMIN cho production.
- `bootstrapShowtimeSeats()` kiểm tra `existsByShowtimeId()` trước → tránh duplicate khi update.
- Khi đổi phòng: ghế cũ bị xóa, booking cũ liên quan sẽ orphan.
- Giá ghế mặc định = basePrice, không phân loại theo loại ghế.
- Mock data (`MOCK_DATES`, `MOCK_THEATERS`) vẫn còn trong code nhưng không dùng.
- Showtime entity có `@ManyToOne(fetch=LAZY)` → cần Open Session In View hoặc `@EntityGraph` để tránh LazyInitializationException khi serialize.
- `date` param từ client là LocalDate → server convert sang LocalDateTime theo server timezone.
