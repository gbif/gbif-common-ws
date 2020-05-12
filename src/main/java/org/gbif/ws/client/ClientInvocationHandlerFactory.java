package org.gbif.ws.client;

import feign.InvocationHandlerFactory;
import feign.Target;
import org.springframework.validation.annotation.Validated;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static feign.Util.checkNotNull;

@SuppressWarnings("unused")
public class ClientInvocationHandlerFactory implements InvocationHandlerFactory {

  @Override
  public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
    return new ClientInvocationHandlerFactory.FeignInvocationHandler(target, dispatch);
  }

  static class FeignInvocationHandler implements InvocationHandler {

    private final Target target;
    private final Map<Method, MethodHandler> dispatch;
    private final ExecutableValidator executableValidator;

    FeignInvocationHandler(Target target, Map<Method, MethodHandler> dispatch) {
      this.target = checkNotNull(target, "target");
      this.dispatch = checkNotNull(dispatch, "dispatch for %s", target);
      this.executableValidator = (ExecutableValidator) Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      validateMethodParameters(proxy, method, args);

      if ("equals".equals(method.getName())) {
        try {
          Object otherHandler =
              args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
          return equals(otherHandler);
        } catch (IllegalArgumentException e) {
          return false;
        }
      } else if ("hashCode".equals(method.getName())) {
        return hashCode();
      } else if ("toString".equals(method.getName())) {
        return toString();
      }

      return getMethodHandler(method).invoke(args);
    }

    private void validateMethodParameters(Object proxy, Method method, Object[] args) {
      if (method.getParameterCount() > 0) {
        Optional<Class<?>[]> groups = Arrays.stream(method.getAnnotations())
            .filter(p -> p.annotationType().equals(Validated.class))
            .map(p -> ((Validated) p).value())
            .findFirst();

        Set<ConstraintViolation<Object>> constraintViolations;
        if (groups.isPresent()) {
          constraintViolations = executableValidator.validateParameters(proxy, method, args, groups.get());
        } else {
          constraintViolations = executableValidator.validateParameters(proxy, method, args);
        }

        if (!constraintViolations.isEmpty()) {
          throw new ConstraintViolationException(constraintViolations);
        }
      }
    }

    private MethodHandler getMethodHandler(Method method) {
      MethodHandler methodHandler = dispatch.get(method);

      // if the result is not null just return the handler
      if (methodHandler != null) {
        return methodHandler;
      }

      // if not we have to find another handler
      Optional<Method> anotherMethod = dispatch.keySet().stream()
          .filter(methodFromDispatch -> !methodFromDispatch.equals(method))
          .filter(methodFromDispatch -> methodFromDispatch.getName().equals(method.getName()))
          .filter(methodFromDispatch -> methodFromDispatch.getParameterCount() == method.getParameterCount())
          .findFirst();

      return anotherMethod.map(dispatch::get)
          .orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ClientInvocationHandlerFactory.FeignInvocationHandler) {
        ClientInvocationHandlerFactory.FeignInvocationHandler other = (ClientInvocationHandlerFactory.FeignInvocationHandler) obj;
        return target.equals(other.target);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return target.hashCode();
    }

    @Override
    public String toString() {
      return target.toString();
    }
  }
}
