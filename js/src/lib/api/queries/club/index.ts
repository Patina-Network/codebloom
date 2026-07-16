import { ApiURL } from "@/lib/api/common/apiURL";
import { sleep } from "@/lib/api/utils/lag";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export const useClubQuery = ({ clubSlug }: { clubSlug: string }) => {
  const apiURL = ApiURL.create("/api/club/{clubSlug}", {
    method: "GET",
    params: {
      clubSlug,
    },
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => fetchClubDto(apiURL),
  });
};

async function fetchClubDto({
  url,
  method,
  res,
}: ApiURL<"/api/club/{clubSlug}", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  return res(await response.json());
}

export const useVerifyPasswordMutation = (clubSlug: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: verifyPassword,
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ApiURL.prefix("/api/auth") });
      queryClient.invalidateQueries({
        queryKey: ApiURL.prefix("/api/club/{clubSlug}", clubSlug),
      });
    },
  });
};

async function verifyPassword({
  userId,
  password,
  clubSlug,
}: {
  userId: string;
  password: string;
  clubSlug: string;
}) {
  const { url, method, req, res } = ApiURL.create("/api/club/verify", {
    method: "POST",
  });
  const response = await fetch(url, {
    method,
    body: req({
      userId,
      password,
      clubSlug,
    }),
  });

  const json = res(await response.json());

  return json;
}
