import { ApiURL } from "@/lib/api/common/apiURL";
import { sleep } from "@/lib/api/utils/lag";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

/**
 * This mutation will set the user's leetcode username, refetching on success
 */
export const useSetLeetcodeUsernameMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateLeetcodeUsername,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ApiURL.prefix("/api/auth") });
    },
  });
};

/**
 * Fetch the user's private key used for Leetcode authentication.
 */
export const useAuthKeyQuery = () => {
  const apiURL = ApiURL.create("/api/leetcode/key", {
    method: "GET",
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => getLeetcodeQueryKey(apiURL),
  });
};

async function updateLeetcodeUsername({
  leetcodeUsername,
}: {
  leetcodeUsername: string;
}) {
  const { url, method, req, res } = ApiURL.create("/api/leetcode/set", {
    method: "POST",
  });
  const response = await fetch(url, {
    method,
    body: req({
      leetcodeUsername,
    }),
  });

  await sleep(350);

  return res(await response.json());
}

async function getLeetcodeQueryKey({
  url,
  method,
  res,
}: ApiURL<"/api/leetcode/key", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(350);

  return res(await response.json());
}
