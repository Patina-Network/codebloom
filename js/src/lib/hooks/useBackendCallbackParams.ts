import { useSearchParams } from "react-router";

/**
 * A hook for parsing backend callback parameters.
 *
 * Useful when the user is redirected from a backend `/api/*` route (e.g., Discord auth or email verification).
 * The backend sends `success` and `message` as URL parameters, which this hook parses into usable values.
 *
 * Returns:
 *   - success: boolean | null
 *   - message: string | null
 */
export function useBackendCallbackParams() {
  const [searchParams] = useSearchParams();

  const success = (() => {
    const val = searchParams.get("success");

    if (val === "true") {
      return true;
    }

    if (val === "false") {
      return false;
    }

    return null;
  })();

  const message = searchParams.get("message");

  return {
    success: success,
    message: message,
  };
}
