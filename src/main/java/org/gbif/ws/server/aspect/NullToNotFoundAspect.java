package org.gbif.ws.server.aspect;

import java.net.URI;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.gbif.api.annotation.NullToNotFound;
import org.gbif.ws.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This aspect throws a {@link NotFoundException} for every {@code null} return value of a method.
 */
@Component
@Aspect
public class NullToNotFoundAspect {

  private static final Logger LOG = LoggerFactory.getLogger(NullToNotFoundAspect.class);

  @AfterReturning(pointcut = "@annotation(org.gbif.api.annotation.NullToNotFound)",
      returning = "retVal")
  public void afterReturningAdvice(JoinPoint jp, Object retVal) {
    if (retVal == null) {
      String uri = ((MethodSignature) jp.getSignature()).getMethod()
          .getAnnotation(NullToNotFound.class).value();

      String[] parameterNames = ((MethodSignature) jp.getSignature()).getParameterNames();
      Object[] parameterValues = jp.getArgs();

      // replace pat variables in URI with values
      for (int i = 0; i < parameterNames.length; i++) {
        if (uri.contains('{' + parameterNames[i] + '}')) {
          uri = uri.replace('{' + parameterNames[i] + '}', parameterValues[i].toString());
        }
      }

      if (uri.contains("{") || uri.contains("}")) {
        LOG.warn("URI was not processed properly and contains special characters {}", uri);
      }

      throw new NotFoundException("Entity not found", URI.create(uri));
    }
  }
}
