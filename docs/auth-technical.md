# Tài liệu kỹ thuật - Chức năng Đăng ký & Đăng nhập

## 1. Mô tả chức năng

Cho phép người dùng tạo tài khoản mới (đăng ký) và truy cập hệ thống (đăng nhập) bằng email + mật khẩu. Sử dụng **JWT (HS256)** cho xác thực, mật khẩu mã hóa **BCrypt**. Token lưu ở client (AsyncStorage), gửi qua header `Authorization: Bearer <token>` cho mọi request cần xác thực.

---

## 2. Danh sách chức năng

| STT | Chức năng | API | Mô tả |
|-----|-----------|-----|-------|
| 1 | Đăng ký | `POST /api/auth/register` | Tạo tài khoản mới (name, email, password) |
| 2 | Đăng nhập | `POST /api/auth/login` | Trả về JWT token + thông tin user |
| 3 | Lấy profile | `GET /api/auth/me` | Cần JWT, trả về thông tin user hiện tại |
| 4 | Kiểm tra đăng nhập | Client-side | Đọc token từ AsyncStorage khi mở app |
| 5 | Đăng xuất | Client-side | Xóa token khỏi AsyncStorage |

---

## 3. Kiến trúc

```
Client (LoginScreen/SignUpScreen) ──▶ authSlice.js (Redux) ──▶ apiClient.js (Axios)
       │ HTTP REST                                              │ Bearer token
       ▼                                                        ▼
AuthController ──▶ UserService ──▶ UserRepository (MySQL)
                     │      │
                     │      └──▶ JwtUtils (tạo/giải mã JWT)
                     └──▶ PasswordEncoder (BCrypt)
```

### Luồng xử lý

**Đăng ký**: Client validate → POST `/api/auth/register` → Kiểm tra trùng email → Mã hóa password → Lưu DB → Trả RegisterResponse.

**Đăng nhập**: Client validate → POST `/api/auth/login` → Tìm user → So sánh password BCrypt → Tạo JWT → Client lưu token vào AsyncStorage + Redux `{isAuthenticated: true}` → RootNavigator chuyển sang UserStack.

**Mỗi request sau đó**: apiClient interceptor đọc token từ AsyncStorage → gắn `Authorization: Bearer <token>` → Spring Security giải mã JWT → cho phép truy cập.

---

## 4. Code

### 4.1 Backend - Các lớp chính

#### `AuthController.java` — `Controllers/`

| Hàm | Endpoint | Mô tả |
|-----|----------|-------|
| `login()` | `POST /api/auth/login` | Gọi `userService.login()`, trả `LoginResponse{token, user}` |
| `register()` | `POST /api/auth/register` | Gọi `userService.registerUser()`, trả `RegisterResponse` |
| `me()` | `GET /api/auth/me` | Lấy email từ SecurityContext, trả `UserResponse` |

#### `UserService.java` — `Services/`

| Hàm | Mô tả |
|-----|-------|
| `registerUser()` | Kiểm tra trùng email → mã hóa BCrypt → lưu User (role="USER", status="ACTIVE") → trả RegisterResponse |
| `login()` | Tìm user theo email → so sánh password → tạo JWT token qua `JwtUtils` → trả LoginResponse{token, user} |
| `getUserProfileByEmail()` | Tìm user → chuyển sang UserResponse (dùng cho API /me) |

#### `JwtUtils.java` — `Utils/`

| Hàm | Mô tả |
|-----|-------|
| `generateToken(email)` | Tạo JWT: subject=email, expiration=24h, ký HS256 |
| `extractEmail(token)` | Giải mã token → lấy email |
| `validateToken(token, email)` | Kiểm tra email khớp + chưa hết hạn |

#### `SecurityConfig.java` — `shared/security/`

Cấu hình Spring Security: (1) Tắt CSRF. (2) CORS mọi origin. (3) Phân quyền URL: `/api/auth/**` → permitAll, `/api/bookings/**` → authenticated, `/api/payment/**` → authenticated. (4) Beans: BCryptPasswordEncoder, JwtDecoder, JwtEncoder.

### 4.2 Backend - Các lớp phụ

| Lớp | Vai trò |
|-----|---------|
| `UserRepository` | `JpaRepository<User, Long>`, phương thức chính: `findUserByEmail()` |
| `AuthRequest` | DTO request login: `{email: @Email, password: @Size(min=5)}` |
| `RegsiterRequest` | DTO request register: `{name, email, password}` |
| `LoginResponse` | DTO: `{token, user: UserResponse}` |
| `RegisterResponse` | DTO: `{id, name, email, message}` |
| `UserResponse` | DTO: `{id, name, email, role, totalSpending, loyaltyPoints, loyaltyTier, ...}` |

### 4.3 Frontend

#### `authSlice.js` — `store/`

Redux Toolkit slice, state:
```javascript
{ user: null, token: null, role: null, isAuthenticated: false, loading: false, error: null }
```

| Async Thunk | Mô tả |
|-------------|-------|
| `registerAsync()` | POST register → chỉ cập nhật loading/error |
| `loginAsync()` | POST login → lưu token vào AsyncStorage + set `isAuthenticated: true` |
| `logoutAsync()` | Xóa AsyncStorage → reset state |
| `checkAuthStatus()` | Đọc token từ AsyncStorage khi mở app → set isAuthenticated |
| `refreshMeAsync()` | GET /me → cập nhật user |

#### `LoginScreen.js` / `SignUpScreen.js`

Màn hình đăng nhập/đăng ký. Validate client-side (email regex, password length) → dispatch Redux thunk → hiển thị lỗi bằng Alert.

#### `apiClient.js` — `services/`

Axios client: (1) Request interceptor tự gắn Bearer token. (2) Response interceptor: 401 → xóa token, 403/5xx → hiển thị lỗi. (3) Base URL: `http://10.0.2.2:8080` (Android), `http://localhost:8080` (iOS).

---

### 4.4 Bảng CSDL - `users`

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | BIGINT, PK, AUTO_INCREMENT | Khóa chính |
| `full_name` | VARCHAR, NOT NULL | Họ tên |
| `email` | VARCHAR, NOT NULL, UNIQUE | Email đăng nhập |
| `password_hash` | VARCHAR, NOT NULL | Mật khẩu BCrypt |
| `role` | VARCHAR, default "USER" | "USER" hoặc "ADMIN" |
| `status` | VARCHAR, default "ACTIVE" | Trạng thái |
| `created_at` / `updated_at` | DATETIME | Timestamps |
| `total_spending` | DOUBLE, default 0.0 | Tổng chi tiêu |
| `loyalty_points` | INT, default 0 | Điểm tích lũy |
| `loyalty_tier` | VARCHAR, default "BRONZE" | Hạng thành viên |
| `tier_updated_at` | DATETIME | Thời gian cập nhật hạng |

---

### 4.5 API Endpoints

| Method | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/auth/register` | `{name, email, password}` | `{id, name, email, message}` | Không |
| POST | `/api/auth/login` | `{email, password}` | `{token, user: {id, name, email, role, ...}}` | Không |
| GET | `/api/auth/me` | - | `{id, name, email, role, ...}` | JWT |

---

## 5. Lưu ý triển khai

- Secret key trong `application.yaml` phải >= 32 bytes cho HS256. Cần thay đổi khi production.
- JWT thời hạn 24h, không có refresh token. Nên giảm xuống 1-2h cho production.
- CORS cho phép mọi origin (`*`) → cần giới hạn khi deploy.
- Password validation: backend `@Size(min=5)`, frontend login `min=5`, frontend register `min=6`.
- 2 cơ chế JWT song song: `JwtUtils` (jjwt) tạo token, `SecurityConfig` (NimbusJOSE) giải mã — tương thích vì cùng secret + HS256.
- `RegsiterRequest` có typo trong tên class.
