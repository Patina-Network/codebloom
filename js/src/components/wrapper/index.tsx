import { BoxRef } from "@/components/wrapper/box";
import { TextRef } from "@/components/wrapper/text";
import { motion } from "motion/react";

export const Text = motion.create(TextRef);
export const Box = motion.create(BoxRef);

export const MM = {
  Text,
  Box,
};
