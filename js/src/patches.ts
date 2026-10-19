/**
 * Collection of functions patched to the `global` namespace.
 *
 * This file must be imported as a side-effect in order to
 * be registered to the `global` namespace.
 */

type KeyToTupleObj<T> = { [K in keyof T]: [K, T[K]] };
type Tuple<T> = KeyToTupleObj<T>[keyof T];

declare global {
  interface ObjectConstructor {
    /**
     * Generic implementation of {@link Object.entries} that will maintain the actual
     * type value of each key and the value.
     */
    typedEntries<const T extends Record<PropertyKey, unknown>>(
      obj: T,
    ): Array<Tuple<T>>;

    /**
     * Generic implementation of {@link Object.keys} that will maintain the actual
     * type value of each key.
     */
    typedKeys<const T extends Record<PropertyKey, unknown>>(
      obj: T,
    ): Array<keyof T>;

    /**
     * Generic implementation of {@link Object.fromEntries} that will maintain the
     * actual type values when converting from the array of tuples into the object.
     */
    typedFromEntries<
      const T extends ReadonlyArray<readonly [PropertyKey, unknown]>,
    >(
      entries: T,
    ): { [K in T[number] as K[0]]: K[1] };
  }
}

Object.typedEntries = function <const T extends Record<PropertyKey, unknown>>(
  obj: T,
): Array<Tuple<T>> {
  return Object.entries(obj) as Array<Tuple<T>>;
};

Object.typedKeys = function <const T extends Record<PropertyKey, unknown>>(
  obj: T,
): Array<keyof T> {
  return Object.keys(obj) as Array<keyof T>;
};

Object.typedFromEntries = function <
  const T extends ReadonlyArray<readonly [PropertyKey, unknown]>,
>(entries: T) {
  return Object.fromEntries(entries) as { [K in T[number] as K[0]]: K[1] };
};

// monkey patch `fetch` to apply `Content-Type: application/json` if not defined.

const originalFetch = window.fetch;

window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
  try {
    const isApi = (() => {
      if (typeof input === "string") {
        return input.startsWith("/api");
      } else if (input instanceof URL) {
        return input.pathname.startsWith("/api");
      }

      // `Request` objects always carry an absolute URL.
      return new URL(input.url).pathname.startsWith("/api");
    })();

    if (isApi) {
      // Per fetch semantics, `init.headers` replaces a `Request`'s own headers.
      const headers = new Headers(
        init?.headers ?? (input instanceof Request ? input.headers : undefined),
      );

      if (!headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
      }

      return originalFetch(input, { ...init, headers });
    }

    return originalFetch(input, init);
  } catch (e) {
    console.error(e);
    throw e;
  }
};
