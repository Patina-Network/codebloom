package org.patinanetwork.codebloom.common.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import java.time.Duration;
import tools.jackson.core.JacksonException;

/** Create and read JWTs. */
public interface JWTClient {
    /** Create the JWT token, along with when it should expire. */
    <T> String encode(T obj, Duration expiresIn) throws JacksonException;

    /** Create the JWT token, default of 15 minute expiration. */
    <T> String encode(T obj) throws JacksonException;

    /** Parse the JWT token back into a valid Object. Will throw if expired or unable to verify JWT. */
    <T> T decode(String token, Class<T> clazz) throws JacksonException, JWTVerificationException;
}
