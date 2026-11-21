import DeactivateAccountModal from "@/app/settings/_components/DeactivateAccountModal";
import CodebloomCard from "@/components/ui/CodebloomCard";
import { Box, Button, Title, Text } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";

export default function DeactivateAccountCard() {
  const [modalOpen, { open: openModal, close: closeModal }] =
    useDisclosure(false);

  return (
    <Box>
      <CodebloomCard>
        <Box m={"md"}>
          <Title order={3} p={"md"}>
            Deactivate Account
          </Title>
          <Text pt={"md"} pl={"md"}>
            Deactivate your account and log out of all devices and browsers.
            While deactivated, you&apos;re hidden from leaderboards and your
            LeetCode submissions stop syncing, but your data is kept. Log back
            in anytime to reactivate.
          </Text>
          <Box pt={"sm"} pl={"md"}>
            <Button mt="sm" color="red" onClick={openModal}>
              Deactivate Account
            </Button>
          </Box>
          <DeactivateAccountModal enabled={modalOpen} close={closeModal} />
        </Box>
      </CodebloomCard>
    </Box>
  );
}
