import axios from "axios";
import { getAccessToken, removeAccessToken } from "../utils/authStorage";

const baseURL = import.meta.env.VITE_API_BASE_URL;

export const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      removeAccessToken();

      const currentPath = window.location.pathname + window.location.search;

      const loginUrl = `/login?redirect=${encodeURIComponent(currentPath)}`;

      if (window.location.pathname !== "/login") {
        window.location.href = loginUrl;
      }
    }

    return Promise.reject(error);
  },
);
