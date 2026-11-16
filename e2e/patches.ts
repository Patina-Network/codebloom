/**
 * LOAD THIS ASAP!!!!
 */

declare global {
  interface ObjectConstructor {
    /**
     * Generic implementation of {@link Object.keys} that will maintain the actual
     * type value of each key.
     */
    typedKeys<const T extends Record<PropertyKey, unknown>>(
      obj: T,
    ): Array<keyof T>;
  }
}

Object.typedKeys = function <const T extends Record<PropertyKey, unknown>>(
  obj: T,
): Array<keyof T> {
  return Object.keys(obj) as Array<keyof T>;
};

export {};
