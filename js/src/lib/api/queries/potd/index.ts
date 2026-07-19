import { ApiURL } from "@/lib/api/common/apiURL";
import { sleep } from "@/lib/api/utils/lag";
import { useQuery } from "@tanstack/react-query";

export const useFetchPotdQuery = () => {
  const apiURL = ApiURL.create("/api/leetcode/potd", {
    method: "GET",
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => fetchPotd(apiURL),
  });
};

async function fetchPotd({
  url,
  method,
  res,
}: ApiURL<"/api/leetcode/potd", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(750);

  const json = res(await response.json());

  return json;
}
