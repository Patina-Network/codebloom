import SchoolEmailModal from "@/app/settings/_components/SchoolEmailModal";
import { schoolVerificationFormSchema } from "@/lib/api/schema/school";
import { TestUtils } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const mutate = vi.fn();
vi.mock("@/lib/api/queries/auth/school", () => ({
  useVerifySchoolMutation: () => ({ mutate, status: "idle" }),
}));

beforeEach(() => mutate.mockReset());

it("expands and collapses CUNY enrollment instructions", async () => {
  const user = userEvent.setup();
  TestUtils.getRenderWithAllProvidersFn()(
    <SchoolEmailModal enabled toggle={vi.fn()} />,
  );
  expect(
    screen.getByRole("dialog", { name: "Verify your student email" }),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Student email")).toBeInTheDocument();
  const help = screen.getByRole("button", {
    name: "How do I enroll if I'm a CUNY student",
  });
  expect(help).toHaveAttribute("aria-expanded", "false");
  await user.click(help);
  expect(help).toHaveAttribute("aria-expanded", "true");
  expect(
    screen.getByText(/@stu-mail.hunter.cuny.edu for Hunter College/),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/sign in to Outlook with your @login.cuny.edu login/),
  ).toBeVisible();
  await user.click(help);
  expect(help).toHaveAttribute("aria-expanded", "false");
  await waitFor(() => {
    expect(
      screen.queryByText(/@stu-mail.hunter.cuny.edu for Hunter College/),
    ).not.toBeVisible();
  });
});

it("blocks a shared CUNY login and allows the student to correct it to an alias", async () => {
  const user = userEvent.setup();
  TestUtils.getRenderWithAllProvidersFn()(
    <SchoolEmailModal enabled toggle={vi.fn()} />,
  );
  const email = screen.getByLabelText("Student email");
  const submit = screen.getByRole("button", { name: "Submit" });
  await user.type(email, "jane.smith03@login.cuny.edu");
  expect(submit).toBeDisabled();
  expect(mutate).not.toHaveBeenCalled();
  await user.clear(email);
  await user.type(email, "jane.smith03@stu-mail.hunter.cuny.edu");
  expect(submit).toBeEnabled();
  await user.click(submit);
  expect(mutate).toHaveBeenCalledWith(
    { email: "jane.smith03@stu-mail.hunter.cuny.edu" },
    expect.any(Object),
  );
});

it.each(["student@LOGIN.CUNY.EDU", "student@cuny.edu"])(
  "explains why %s needs a college alias",
  (email) => {
    const parsed = schoolVerificationFormSchema.safeParse({ email });
    expect(parsed.success).toBe(false);
    if (!parsed.success)
      expect(parsed.error.issues[0].message).toContain("college email alias");
  },
);

it.each(["student@nyu.edu", "student@stu-mail.baruch.cuny.edu"])(
  "continues accepting %s",
  (email) => {
    expect(schoolVerificationFormSchema.safeParse({ email }).success).toBe(
      true,
    );
  },
);
