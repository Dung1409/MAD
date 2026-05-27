import { apiClient } from './apiClient';

export const userManagementService = {
  // Get all users with filters
  getAllUsers: async (search = '', role = '', status = '', loyaltyTier = '') => {
    try {
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (role) params.append('role', role);
      if (status) params.append('status', status);
      if (loyaltyTier) params.append('loyaltyTier', loyaltyTier);
      
      const response = await apiClient.get(`/api/admin/users?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching users:', error);
      throw error;
    }
  },

  // Get user by ID
  getUserById: async (userId) => {
    try {
      const response = await apiClient.get(`/api/admin/users/${userId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching user:', error);
      throw error;
    }
  },

  // Update user role
  updateUserRole: async (userId, role) => {
    try {
      const response = await apiClient.put(`/api/admin/users/${userId}/role?role=${role}`);
      return response.data;
    } catch (error) {
      console.error('Error updating user role:', error);
      throw error;
    }
  },

  // Update user status (block/unblock)
  updateUserStatus: async (userId, status) => {
    try {
      const response = await apiClient.put(`/api/admin/users/${userId}/status?status=${status}`);
      return response.data;
    } catch (error) {
      console.error('Error updating user status:', error);
      throw error;
    }
  },

  // Delete user and preserve booking history snapshots
  deleteUser: async (userId) => {
    try {
      await apiClient.delete(`/api/admin/users/${userId}`);
    } catch (error) {
      console.error('Error deleting user:', error);
      throw error;
    }
  },

  // Get user bookings
  getUserBookings: async (userId, status = '') => {
    try {
      const params = new URLSearchParams();
      if (status) params.append('status', status);
      const query = params.toString();
      const response = await apiClient.get(`/api/admin/users/${userId}/bookings${query ? `?${query}` : ''}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching user bookings:', error);
      throw error;
    }
  }
};
