/**
 * Convert a number into an ordinal.
 *
 * For example:
 * n = 2 -> 2nd
 * n = 4 -> 4th
 * n = 13 -> 13th
 */
export default function getOrdinal(n: number): OrdinalString {
  const remainder10 = n % 10;
  const remainder100 = n % 100;

  let suffix: OrdinalSuffix = "th";
  if (remainder10 === 1 && remainder100 !== 11) {
    suffix = "st";
  } else if (remainder10 === 2 && remainder100 !== 12) {
    suffix = "nd";
  } else if (remainder10 === 3 && remainder100 !== 13) {
    suffix = "rd";
  }

  return `${n}${suffix}`;
}

export type OrdinalString =
  `${number}th` | `${number}st` | `${number}nd` | `${number}rd`;

export type OrdinalSuffix = "th" | "st" | "nd" | "rd";
