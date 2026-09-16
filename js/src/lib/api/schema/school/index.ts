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
      "Please make sure to use your college email alias (e.g., jane.smith03@stu-mail.hunter.cuny.edu) so we can identify your school.",
    ),
});
