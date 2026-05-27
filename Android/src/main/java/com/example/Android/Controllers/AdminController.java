package com.example.Android.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.Android.Services.AdminService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * Get dashboard statistics
     * Returns: totalRevenue, totalBookings, totalUsers, totalMovies, recentBookings
     */
    @GetMapping("/dashboard/stats")
    // @PreAuthorize("hasRole('ADMIN')")  // Temporarily disabled for testing
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = adminService.getDashboardStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get revenue statistics by date range
     */
    @GetMapping("/dashboard/revenue")
    // @PreAuthorize("hasRole('ADMIN')")  // Temporarily disabled for testing
    public ResponseEntity<?> getRevenueStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> stats = adminService.getRevenueStatistics(startDate, endDate);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get top movies by bookings
     */
    @GetMapping("/dashboard/top-movies")
    // @PreAuthorize("hasRole('ADMIN')")  // Temporarily disabled for testing
    public ResponseEntity<?> getTopMovies(@RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(adminService.getTopMovies(limit));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all bookings with pagination (legacy endpoint for dashboard)
     */
    @GetMapping("/dashboard/bookings")
    // @PreAuthorize("hasRole('ADMIN')")  // Temporarily disabled for testing
    public ResponseEntity<?> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(adminService.getAllBookings(page, size));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
