import {
  Box,
  Button,
  CloseButton,
  Flex,
  Modal,
  Text,
  Title,
} from "@mantine/core";
import { Link } from "react-router-dom";

type DeactivateAccountModalProps = {
  enabled: boolean;
  close: () => void;
};

export default function LogoutAllSessionsModal({
  enabled,
  close,
}: Readonly<DeactivateAccountModalProps>) {
  return (
    <Modal
      title="Deactivate Account"
      opened={enabled}
      onClose={close}
      withCloseButton={false}
    >
      <Flex justify="space-between" align="center" mb="md">
        <Title order={4}>Deactivate Account</Title>
        <CloseButton aria-label="Close deactivation modal" onClick={close} />
      </Flex>
      <Box p={"lg"}>
        <Text>
          Are you sure you want to deactivate your account? You'll be logged out
          on all devices and browsers, hidden from leaderboards, and your
          LeetCode submissions will stop syncing. Your data is kept, so you can
          log back in anytime to reactivate.
        </Text>
        <Flex gap="sm" justify="flex-end" mt="md">
          <Button variant="default" onClick={close}>
            Cancel
          </Button>
          <Button
            component={Link}
            to={"/api/auth/deactivate"}
            reloadDocument
            color="red"
          >
            Deactivate Account
          </Button>
        </Flex>
      </Box>
    </Modal>
  );
}
