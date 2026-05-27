import { apiClient } from './apiClient';

const adminService = {
  // Get dashboard statistics
  getDashboardStats: async () => {
    try {
      const response = await apiClient.get('/api/admin/dashboard/stats');
      return response.data;
    } catch (error) {
      console.error('Error fetching dashboard stats:', error);
      throw error;
    }
  },

  // Get revenue statistics
  getRevenueStats: async (startDate, endDate) => {
    try {
      const params = {};
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      
      const response = await apiClient.get('/api/admin/dashboard/revenue', { params });
      return response.data;
    } catch (error) {
      console.error('Error fetching revenue stats:', error);
      throw error;
    }
  },

  // Get top movies
  getTopMovies: async (limit = 5) => {
    try {
      const response = await apiClient.get('/api/admin/dashboard/top-movies', {
        params: { limit }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching top movies:', error);
      throw error;
    }
  },

  // Get all bookings with pagination (for dashboard)
  getAllBookings: async (page = 0, size = 20) => {
    try {
      const response = await apiClient.get('/api/admin/dashboard/bookings', {
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching all bookings:', error);
      throw error;
    }
  },
};

export default adminService;