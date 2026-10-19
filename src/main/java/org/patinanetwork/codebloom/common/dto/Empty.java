package org.patinanetwork.codebloom.common.dto;

import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Use {@code Empty.of()} when you want to return a successful response, but have no data to send back to the client.
 *
 * <p>The client will only see an empty object, which is more ideal than an undefined key.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public ResponseEntity<ApiResponder<Empty>> exampleFunction() {
 *     // ...
 *     return ResponseEntity.ok(ApiResponder.success(Empty.of())); // returns an empty object: {}
 * }
 * }</pre>
 */
@JsonSerialize
public final class Empty {

    private static final Empty INSTANCE = new Empty();

    private Empty() {}

    /**
     * Use {@code Empty.of()} when you want to return a successful response, but have no data to send back to the
     * client.
     *
     * <p>The client will only see an empty object, which is more ideal than an undefined key.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * public ResponseEntity<ApiResponder<Empty>> exampleFunction() {
     *     // ...
     *     return ResponseEntity.ok(ApiResponder.success(Empty.of())); // returns an empty object: {}
     * }
     * }</pre>
     */
    public static Empty of() {
        return INSTANCE;
    }
}
