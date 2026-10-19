import PrettyCounter from "@/components/ui/pretty-counter/PrettyCounter";
import useCountdown from "@/lib/hooks/useCountdown";
import { Flex, Text, Transition } from "@mantine/core";
import { useMounted } from "@mantine/hooks";
import { useState } from "react";

export default function Banner({
  message,
  counter,
}: {
  message: string;
  counter?: Date;
}) {
  const mounted = useMounted();

  /**
   * Seconds remaining at mount: -1 if not exists, 0 if expired.
   * Read once in a lazy initializer so render stays pure.
   */
  const [time] = useState(() => {
    if (!counter) {
      return -1;
    }

    return Math.floor((counter.getTime() - Date.now()) / 1000);
  });
  const [countdown] = useCountdown(time);
  // Hide once a running timer reaches 0.
  const expired = time >= 0 && countdown === 0;

  return (
    !expired && (
      <Transition keepMounted mounted={mounted} transition={"scale-y"}>
        {(style) => (
          <Flex
            bg={"green"}
            ta={"center"}
            direction={"column"}
            align={"center"}
            style={style}
            justify={"center"}
            gap={"sm"}
            p={"10px"}
          >
            <Text size={"lg"}>{message}</Text>
            {counter && <PrettyCounter size={"lg"} time={countdown} />}
          </Flex>
        )}
      </Transition>
    )
  );
}
