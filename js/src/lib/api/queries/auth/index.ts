import { ApiURL } from "@/lib/api/common/apiURL";
import { sleep } from "@/lib/api/utils/lag";
import { useQuery } from "@tanstack/react-query";

/**
 * Fetch the user's authentication state from the server.
 */
export const useAuthQuery = () => {
  const apiURL = ApiURL.create("/api/auth/validate", {
    method: "GET",
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => validateAuthentication(apiURL),
  });
};

async function validateAuthentication({
  url,
  method,
  res,
}: ApiURL<"/api/auth/validate", "get">) {
  const response = await fetch(url, {
    method,
  });

  const json = res(await response.json());

  if (json.success) {
    await sleep(350);
    return {
      session: json.payload.session,
      user: json.payload.user,
      isAdmin: json.payload.user.admin,
    };
  }

  return { session: undefined, user: undefined, isAdmin: false };
}
