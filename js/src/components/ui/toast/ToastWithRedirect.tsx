import { DefaultMantineColor } from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { ReactNode, useEffect } from "react";
import { useNavigate } from "react-router";

/**
 * A custom React function that shows a notification and handles the redirect in a React way.
 * The alternative would be to pollute the codebase with useEffects and repetitive checks, which are
 * unfavorable and hard to maintain.
 */
export default function ToastWithRedirect({
  to,
  message,
  color,
}: {
  to: string | number;
  message: ReactNode;
  color?: DefaultMantineColor;
}) {
  const navigate = useNavigate();

  useEffect(() => {
    notifications.show({
      message,
      color,
    });
    if (typeof to === "string") {
      navigate(to);
    } else {
      navigate(to);
    }
  }, [color, message, navigate, to]);

  return <></>;
}
