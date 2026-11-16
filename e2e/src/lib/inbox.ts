import { ImapFlow } from "imapflow";
import { simpleParser } from "mailparser";
import { setTimeout as sleep } from "node:timers/promises";

import { requireEnv } from "@/lib/env";

// Gmail sometimes files first-time senders as spam.
const MAILBOXES = ["INBOX", "[Gmail]/Spam"];

export function uniqueInboxAddress(tag: string): string {
  const username = requireEnv("E2E_EMAIL_USERNAME");
  const [local, domain] = username.split("@");

  if (domain?.toLowerCase() !== "gmail.com") {
    throw new Error(
      `E2E_EMAIL_USERNAME must be a @gmail.com address (the e2e profile only allows @gmail.com), got ${username}.`,
    );
  }

  return `${local}+${tag}-${Date.now()}@${domain}`;
}

export async function waitForEmailHtml({
  to,
  timeoutMs,
}: {
  to: string;
  timeoutMs: number;
}): Promise<string> {
  const client = new ImapFlow({
    host: requireEnv("E2E_EMAIL_IMAP_HOST"),
    port: Number(requireEnv("E2E_EMAIL_IMAP_PORT")),
    secure: true,
    auth: {
      user: requireEnv("E2E_EMAIL_USERNAME"),
      pass: requireEnv("E2E_EMAIL_PASSWORD"),
    },
    logger: false,
  });

  await client.connect();

  try {
    const deadline = Date.now() + timeoutMs;

    while (Date.now() < deadline) {
      for (const mailbox of MAILBOXES) {
        const lock = await client.getMailboxLock(mailbox);

        try {
          const uids = await client.search({ to }, { uid: true });
          const uid = uids ? uids.at(-1) : undefined;

          if (uid === undefined) {
            continue;
          }

          const message = await client.fetchOne(
            String(uid),
            { source: true },
            { uid: true },
          );

          if (message && message.source) {
            const parsed = await simpleParser(message.source);

            if (parsed.html) {
              return parsed.html;
            }
          }
        } finally {
          lock.release();
        }
      }

      await sleep(5_000);
    }
  } finally {
    await client.logout();
  }

  throw new Error(
    `No email to ${to} within ${timeoutMs / 1000}s. Check the backend's CODEBLOOM_EMAIL_* SMTP settings and the E2E inbox.`,
  );
}
