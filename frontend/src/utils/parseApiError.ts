import axios from "axios";
import type { ApiErrorResponse, ParsedApiError } from "../types/api";

export function parseApiError(error: unknown): ParsedApiError {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return {
      message: "Произошла неизвестная ошибка",
    };
  }

  const status = error.response?.status;
  const responseData = error.response?.data;

  if (!error.response || status === 503) {
    return {
      message: "Не удалось подключиться к серверу",
    };
  }

  if (responseData?.error) {
    return {
      status,
      message: responseData.error,
    };
  }

  return {
    status,
    message: "Не удалось выполнить запрос",
  };
}
