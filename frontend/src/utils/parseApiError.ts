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
  const errorMessage = responseData?.error;

  if (!error.response || status === 503) {
    return {
      message: "Не удалось подключиться к серверу",
    };
  }

  if (errorMessage?.startsWith("Method parameter")) {
    return {
      status,
      message: "Указано неверное значение",
    };
  }

  if (status === 500) {
    return {
      status,
      message: "Произошла внутренняя ошибка",
    };
  }

  if (errorMessage) {
    return {
      status,
      message: errorMessage,
    };
  }

  return {
    status,
    message: "Не удалось выполнить запрос",
  };
}
