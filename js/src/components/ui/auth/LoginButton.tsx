import { Button } from "@mantine/core";
import { FaDiscord } from "react-icons/fa";
import { Link } from "react-router";

export default function LoginButton({ className }: { className?: string }) {
  return (
    <Link to="/api/auth/flow/discord" reloadDocument>
      <Button
        color="#5865F2"
        fullWidth
        leftSection={<FaDiscord size={14} />}
        radius="md"
        className={className}
      >
        Login to Discord
      </Button>
    </Link>
  );
}
