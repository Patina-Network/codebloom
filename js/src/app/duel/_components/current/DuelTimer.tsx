import { Text } from "@mantine/core";
import { useState, useEffect } from "react";

interface DuelTimerProps {
  endTime: number; // milliseconds
}

const DuelTimer = ({ endTime }: DuelTimerProps) => {
  const getTimeRemaining = () => {
    const total = endTime - Date.now();
    const seconds = Math.floor((total / 1000) % 60);
    const minutes = Math.floor((total / 1000 / 60) % 60);
    if (total <= 0) {
      return {
        minutes: 0,
        seconds: 0,
      };
    }
    return {
      minutes,
      seconds,
    };
  };

  const [timeLeft, setTimeLeft] = useState(() => getTimeRemaining());
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(getTimeRemaining());
    }, 1);

    return () => clearInterval(timer);
  });

  return (
    <div>
      <Text size="xl">
        {timeLeft.minutes}:{timeLeft.seconds.toString().padStart(2, "0")}
      </Text>
    </div>
  );
};

export default DuelTimer;
