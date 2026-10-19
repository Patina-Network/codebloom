import { ErrorResponse } from "react-router";

/**
 * Custom React Router error object that may also include an error object.
 */
export type CustomErrorResponse = ErrorResponse & {
  error?: {
    message?: string;
    stack?: string;
  };
};
