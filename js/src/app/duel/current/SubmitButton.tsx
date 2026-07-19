import { Button } from "@mantine/core";

export default function SubmitButton() {
  const handleSubmit = () => {
    console.log("Test");
  };

  return (
    <Button color="green" onClick={handleSubmit}>
      Submit
    </Button>
  );
}
