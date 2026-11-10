package org.patinanetwork.codebloom.common.ff;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.patinanetwork.codebloom.common.ff.annotation.FF;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
@Slf4j
public class FFAspect {

    private final FeatureFlagManager featureFlagManager;
    private final ExpressionParser parser;

    public FFAspect(final FeatureFlagManager featureFlagManager) {
        this.featureFlagManager = featureFlagManager;
        this.parser = new SpelExpressionParser();
    }

    @Around("@annotation(ff)")
    public Object gateMethodByFeatureFlags(final ProceedingJoinPoint joinPoint, final FF ff) throws Throwable {
        String expression = ff.value();

        var flags = featureFlagManager.getFlags();
        StandardEvaluationContext context = new StandardEvaluationContext(flags);
        context.addPropertyAccessor(new MapAccessor());

        Boolean isEnabled;
        try {
            isEnabled = parser.parseExpression(expression).getValue(context, Boolean.class);
        } catch (Exception e) {
            log.error("Invalid @FF expression: " + expression, e);
            throw new IllegalArgumentException("Invalid @FF expression: " + expression, e);
        }

        if (!Boolean.TRUE.equals(isEnabled)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Endpoint is not available. Feature flag expression evaluated to false: " + expression);
        }

        return joinPoint.proceed();
    }
}
