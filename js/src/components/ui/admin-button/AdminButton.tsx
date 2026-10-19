import { useAuthQuery } from "@/lib/api/queries/auth";
import { Button } from "@mantine/core";
import { Link } from "react-router";

export function GotoAdminPageButton() {
  const { data, status } = useAuthQuery();

  if (status !== "success") {
    return null;
  }

  if (!data.isAdmin) {
    return null;
  }

  return (
    <Button component={Link} to="/admin" mt={0} size="xs" variant="outline">
      Admin
    </Button>
  );
}
