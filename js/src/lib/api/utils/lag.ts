export async function sleep(ms: number) {
  if (import.meta.env.MODE === "test") return;
  return await new Promise<void>((resolve) => {
    setTimeout(() => {
      resolve(void 0);
    }, ms);
  });
}
