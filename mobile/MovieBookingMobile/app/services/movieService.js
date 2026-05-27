import { apiClient } from './apiClient';

// Nhóm API phim, trong đó có các endpoint hỗ trợ luồng gợi ý.
export const movieService = {
  getAllMovies: async (params = {}) => {
    try {
      console.log('movieService.getAllMovies called with params:', params);
      const response = await apiClient.get('/api/movies', { params });
      console.log('Movies API response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error fetching movies:', error);
      throw error;
    }
  },

  getMovieById: async (id) => {
    try {
      const response = await apiClient.get(`/api/movies/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching movie ${id}:`, error);
      throw error;
    }
  },

  searchMovies: async (keyword) => {
    try {
      const response = await apiClient.get('/api/movies/search', {
        params: { keyword }
      });
      return response.data;
    } catch (error) {
      console.error('Error searching movies:', error);
      throw error;
    }
  },

  getMoviesByGenre: async (genre) => {
    try {
      const response = await apiClient.get(`/api/movies/genre/${genre}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching movies by genre ${genre}:`, error);
      throw error;
    }
  },

  getFeaturedMovies: async () => {
    try {
      const response = await apiClient.get('/api/movies/featured');
      return response.data;
    } catch (error) {
      console.error('Error fetching featured movies:', error);
      throw error;
    }
  },

  /**
  * Lấy danh sách thể loại để người dùng chọn làm dữ liệu gợi ý.
  * Output: response.data từ /api/recommendations/genres.
   */
  getRecommendationGenres: async () => {
    try {
      const response = await apiClient.get('/api/recommendations/genres');
      return response.data;
    } catch (error) {
      console.error('Error fetching recommendation genres:', error);
      throw error;
    }
  },

  /**
  * Gửi các thể loại người dùng đã chọn lên server để lưu preference.
  * Input: danh sách id thể loại; Output: response.data từ /api/recommendations/select/genres.
   */
  selectRecommendationGenres: async (genreIds = []) => {
    try {
      const body = {
        genres: genreIds.map((id) => ({ id })),
      };
      const response = await apiClient.post('/api/recommendations/select/genres', body);
      return response.data;
    } catch (error) {
      console.error('Error saving selected recommendation genres:', error);
      throw error;
    }
  },

  /**
  * Lấy danh sách phim gợi ý theo hạn mức.
  * Output: response.data từ /api/recommendations/movies.
   */
  getRecommendedMovies: async (limit = 10) => {
    try {
      const response = await apiClient.get('/api/recommendations/movies', {
        params: { limit },
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching recommended movies:', error);
      throw error;
    }
  },

  /**
  * Thông báo server về phim người dùng vừa xem/nhấn, để cập nhật tương tác.
  * Tác dụng phụ: ghi history + tương tác (backend) và có thể phát sự kiện gợi ý.
   */
  selectMovieForRecommendation: async (movieId) => {
    try {
      const response = await apiClient.post('/api/movies/select', { movieId });
      return response.data;
    } catch (error) {
      console.error('Error saving movie selection:', error);
      throw error;
    }
  },

  // ============ ADMIN CRUD METHODS ============
  
  createMovie: async (movieData) => {
    try {
      const response = await apiClient.post('/api/admin/movies', movieData);
      return response.data;
    } catch (error) {
      console.error('Error creating movie:', error);
      throw error;
    }
  },

  updateMovie: async (id, movieData) => {
    try {
      const response = await apiClient.put(`/api/admin/movies/${id}`, movieData);
      return response.data;
    } catch (error) {
      console.error(`Error updating movie ${id}:`, error);
      throw error;
    }
  },

  deleteMovie: async (id) => {
    try {
      await apiClient.delete(`/api/admin/movies/${id}`);
    } catch (error) {
      console.error(`Error deleting movie ${id}:`, error);
      throw error;
    }
  },

  getMoviesByStatus: async (status) => {
    try {
      const response = await apiClient.get(`/api/admin/movies/status/${status}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching movies by status ${status}:`, error);
      throw error;
    }
  }
};

export const genreService = {
  getAllGenres: async () => {
    try {
      const response = await apiClient.get('/api/genres');
      return response.data;
    } catch (error) {
      console.error('Error fetching genres:', error);
      throw error;
    }
  }
};

