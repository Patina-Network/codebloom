import { Box, BoxProps } from "@mantine/core";
import { forwardRef } from "react";

interface IBoxProps
  extends
    BoxProps,
    Omit<React.ComponentPropsWithoutRef<"div">, keyof BoxProps> {}

export const BoxRef = forwardRef<HTMLDivElement, IBoxProps>(
  ({ children, ...props }, ref) => {
    return (
      <Box ref={ref} {...props}>
        {children}
      </Box>
    );
  },
);
