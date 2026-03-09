export type ApiErrorResponse = {
  error?: string;
  timestamp?: string;
};

export type ParsedApiError = {
  status?: number;
  message: string;
};
