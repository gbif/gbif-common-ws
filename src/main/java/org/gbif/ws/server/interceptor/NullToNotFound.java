package org.gbif.ws.server.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a jersey resource method to throw a NotFoundException (becomes http 404)
 * when null is returned by the method. Used by the guice ws modules to bind the NullToNotFoundInterceptor
 * to selective methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullToNotFound {

}
