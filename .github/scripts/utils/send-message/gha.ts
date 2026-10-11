import { sendMessage } from "@/utils/send-message";

const prId = (() => {
  const v = process.env.PR_ID;
  if (!v) {
    throw new Error("PR_ID is required");
  }
  const n = Number(v);
  if (Number.isNaN(n)) {
    throw new Error("PR_ID must be a number");
  }
  return n;
})();

const message = (() => {
  const v = process.env.MESSAGE;
  if (!v) {
    throw new Error("MESSAGE is required");
  }
  return v;
})();

/**
 * @deprecated usable but is not recommended.
 */
export async function main() {
  await sendMessage(prId, message);
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
