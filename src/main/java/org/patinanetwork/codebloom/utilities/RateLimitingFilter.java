package org.patinanetwork.codebloom.utilities;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedis;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisProvider;
import org.patinanetwork.codebloom.common.simpleredis.SimpleRedisSlot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("!ci")
public class RateLimitingFilter implements Filter {

    private static final long API_RATE_LIMIT_CAPACITY = 15L;
    private static final long API_REFILL_INTERVAL_MILLIS = 1000L;

    private static final long STATIC_RATE_LIMIT_CAPACITY = 100L;
    private static final long STATIC_REFILL_INTERVAL_MILLIS = 1000L;

    private final SimpleRedis<Bucket> simpleRedis;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(final SimpleRedisProvider simpleRedisProvider, final ObjectMapper objectMapper) {
        this.simpleRedis = simpleRedisProvider.select(SimpleRedisSlot.GLOBAL_RATE_LIMIT);
        this.objectMapper = objectMapper;
    }

    private Bucket createNewBucket(final long capacity, final long refillIntervalMillis) {
        var bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofMillis(refillIntervalMillis))
                .build();

        return Bucket.builder().addLimit(bandwidth).build();
    }

    @Override
    public final void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();
        String remoteAddr = httpRequest.getRemoteAddr();

        boolean isApiPath = path.startsWith("/api");
        final long rateLimitCapacity = isApiPath ? API_RATE_LIMIT_CAPACITY : STATIC_RATE_LIMIT_CAPACITY;
        final long refillInterval = isApiPath ? API_REFILL_INTERVAL_MILLIS : STATIC_REFILL_INTERVAL_MILLIS;
        String bucketKey = remoteAddr + (isApiPath ? ":api" : ":static");

        Bucket bucket;
        synchronized (this) {
            bucket = simpleRedis.get(bucketKey);
            if (bucket == null) {
                simpleRedis.put(bucketKey, createNewBucket(rateLimitCapacity, refillInterval));
                bucket = simpleRedis.get(bucketKey);
            }
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            var apiResponder = ApiResponder.failure("Too Many Requests");

            httpResponse.setContentType("application/json");
            httpResponse.setStatus(429);
            objectMapper.writeValue(httpResponse.getWriter(), apiResponder);
        }
    }
}
