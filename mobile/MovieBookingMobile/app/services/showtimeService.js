import { apiClient } from './apiClient';

export const showtimeService = {
  // Get all showtimes
  getAllShowtimes: async (filters = {}) => {
    try {
      const params = new URLSearchParams();
      if (filters.movieId) params.append('movieId', filters.movieId);
      if (filters.date) params.append('date', filters.date);
      if (filters.cinemaId) params.append('cinemaId', filters.cinemaId);
      
      const url = `/api/showtimes${params.toString() ? '?' + params.toString() : ''}`;
      console.log('Fetching showtimes:', url);
      
      const response = await apiClient.get(url);
      return response.data;
    } catch (error) {
      console.error('Error fetching showtimes:', error);
      throw error;
    }
  },

  // Get showtime by ID
  getShowtimeById: async (showtimeId) => {
    try {
      const response = await apiClient.get(`/api/showtimes/${showtimeId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching showtime:', error);
      throw error;
    }
  },

  // Create showtime
  createShowtime: async (showtimeData) => {
    try {
      const response = await apiClient.post('/api/showtimes', showtimeData);
      return response.data;
    } catch (error) {
      console.error('Error creating showtime:', error);
      throw error;
    }
  },

  // Update showtime
  updateShowtime: async (id, showtimeData) => {
    try {
      const response = await apiClient.put(`/api/showtimes/${id}`, showtimeData);
      return response.data;
    } catch (error) {
      console.error('Error updating showtime:', error);
      throw error;
    }
  },

  // Delete showtime
  deleteShowtime: async (id) => {
    try {
      await apiClient.delete(`/api/showtimes/${id}`);
    } catch (error) {
      console.error('Error deleting showtime:', error);
      throw error;
    }
  },

  // Legacy method for compatibility
  getShowtimes: async (filters = {}) => {
    return showtimeService.getAllShowtimes(filters);
  },
};

// Cinema Service
export const cinemaService = {
  getAllCinemas: async () => {
    try {
      const response = await apiClient.get('/api/cinemas');
      return response.data;
    } catch (error) {
      console.error('Error fetching cinemas:', error);
      throw error;
    }
  },

  getCinemaById: async (id) => {
    try {
      const response = await apiClient.get(`/api/cinemas/${id}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching cinema:', error);
      throw error;
    }
  },
};

export const roomService = {
  getRoomsByCinema: async (cinemaId) => {
    try {
      const response = await apiClient.get('/api/rooms', {
        params: { cinemaId },
      });
      return response.data;
    } catch (error) {
      if (error?.status === 404) {
        return [];
      }
      console.error('Error fetching rooms by cinema:', error);
      throw error;
    }
  },
};

export default showtimeService;
