import {
  Box,
  Button,
  CloseButton,
  Flex,
  Modal,
  Text,
  Title,
} from "@mantine/core";
import { Link } from "react-router";

type LogoutAllSessionsModalProps = {
  enabled: boolean;
  close: () => void;
};

export default function LogoutAllSessionsModal({
  enabled,
  close,
}: LogoutAllSessionsModalProps) {
  return (
    <Modal opened={enabled} onClose={close} withCloseButton={false}>
      <Flex justify="space-between" align="center" mb="md">
        <Title order={4}>Log Out All Sessions</Title>
        <CloseButton onClick={close} />
      </Flex>
      <Box p={"lg"}>
        <Text>
          Are you sure you want to log out of all sessions? This will sign you
          out on all devices and browsers.
        </Text>
        <Flex gap="sm" justify="flex-end" mt="md">
          <Button variant="default" onClick={close}>
            Cancel
          </Button>
          <Button
            component={Link}
            to={"/api/auth/logout/all"}
            reloadDocument
            color="red"
          >
            Log Out All Sessions
          </Button>
        </Flex>
      </Box>
    </Modal>
  );
}
