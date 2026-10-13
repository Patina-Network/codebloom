import { ApiURL } from "@/lib/api/common/apiURL";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export const useIncompleteQuestionQuery = () => {
  const apiURL = ApiURL.create("/api/admin/questions/incomplete", {
    method: "GET",
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => fetchIncompleteQuestions(apiURL),
  });
};

async function fetchIncompleteQuestions({
  url,
  method,
  res,
}: ApiURL<"/api/admin/questions/incomplete", "get">) {
  const response = await fetch(url, {
    method,
  });

  return res(await response.json());
}

/**
 * Queue a new job for every incomplete question without an active job.
 */
export const useRequeueIncompleteQuestionsMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: requeueIncompleteQuestions,
    onSuccess: async (data) => {
      if (data.success) {
        queryClient.invalidateQueries({
          queryKey: ApiURL.prefix("/api/admin/questions/incomplete"),
        });
      }
    },
  });
};

async function requeueIncompleteQuestions() {
  const { url, method, res } = ApiURL.create(
    "/api/admin/questions/incomplete/requeue",
    {
      method: "POST",
    },
  );
  const response = await fetch(url, {
    method,
  });

  return res(await response.json());
}
