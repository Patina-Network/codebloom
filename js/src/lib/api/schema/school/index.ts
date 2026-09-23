import { z } from "zod";

export const schoolVerificationFormSchema = z.object({
  email: z
    .string()
    .trim()
    .email()
    .min(1)
    .max(230)
    .refine(
      (email) =>
        !["login.cuny.edu", "cuny.edu"].includes(
          email.split("@")[1]?.toLowerCase(),
        ),
      "CUNY students: use your college email alias (for example, firstname.lastname##@stu-mail.hunter.cuny.edu). " +
        "Then sign in to Outlook with your @login.cuny.edu credentials to open the verification link.",
    ),
});
