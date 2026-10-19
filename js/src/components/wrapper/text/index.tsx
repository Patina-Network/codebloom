import { Text, TextProps } from "@mantine/core";
import { forwardRef } from "react";

interface ITextProps
  extends
    TextProps,
    Omit<React.ComponentPropsWithoutRef<"p">, keyof TextProps> {}

export const TextRef = forwardRef<HTMLDivElement, ITextProps>(
  ({ children, ...props }, ref) => {
    return (
      <Text ref={ref} {...props}>
        {children}
      </Text>
    );
  },
);
