import { Button, Group, Modal, Text } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";

export default function ForfeitButton() {
  const [opened, { open, close }] = useDisclosure(false);

  const handleConfirm = () => {
    close();
  };

  return (
    <>
      <Button color="red" onClick={open}>
        Forfeit
      </Button>
      <Modal
        opened={opened}
        onClose={close}
        withCloseButton={false}
        centered
        size="sm"
      >
        <Text mb="md" ta="center">
          Are you sure you want to Forfeit?
        </Text>
        <Group justify="center">
          <Button color="red" onClick={close}>
            ✕
          </Button>
          <Button color="green" onClick={handleConfirm}>
            ✓
          </Button>
        </Group>
      </Modal>
    </>
  );
}
