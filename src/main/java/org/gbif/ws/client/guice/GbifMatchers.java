package org.gbif.ws.client.guice;

import org.gbif.ws.util.spring.AnnotationUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;

/**
 * AnnotatedWithType matcher implementation based on guice Matchers, but using the Spring AnnotationUtils to
 * match annotations on parameterized base classes such as the BaseWsGetClient.get(K key).
 */
public class GbifMatchers {

  private GbifMatchers() {
  }


  /**
   * Returns a matcher which matches elements (methods, classes, etc.)
   * with a given annotation.
   */
  public static Matcher<AnnotatedElement> annotatedWith(final Class<? extends Annotation> annotationType) {
    return new AnnotatedWithType(annotationType);
  }

  private static void checkForRuntimeRetention(Class<? extends Annotation> annotationType) {
    Retention retention = annotationType.getAnnotation(Retention.class);
    checkArgument(retention != null && retention.value() == RetentionPolicy.RUNTIME,
      "Annotation " + annotationType.getSimpleName() + " is missing RUNTIME retention");
  }

  private static void checkArgument(boolean b, String s) {
    if (!b) {
      throw new IllegalArgumentException(s);
    }
  }

  private static Class<? extends Annotation> checkNotNull(Class<? extends Annotation> arg, String s) {
    if (arg == null) {
      throw new IllegalArgumentException(s);
    }
    return arg;
  }


  private static class AnnotatedWithType extends AbstractMatcher<AnnotatedElement> implements Serializable {

    private final Class<? extends Annotation> annotationType;

    public AnnotatedWithType(Class<? extends Annotation> annotationType) {
      this.annotationType = checkNotNull(annotationType, "annotation type");
      checkForRuntimeRetention(annotationType);
    }

    public boolean matches(AnnotatedElement element) {
      if (element instanceof Method) {
        Method m = (Method) element;
        if (m.isBridge()) {
          return AnnotationUtils.getAnnotation(m, annotationType) != null;
        }
      }

      return element.getAnnotation(annotationType) != null;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof AnnotatedWithType && ((AnnotatedWithType) other).annotationType.equals(annotationType);
    }

    @Override
    public int hashCode() {
      return 37 * annotationType.hashCode();
    }

    @Override
    public String toString() {
      return "annotatedWith(" + annotationType.getSimpleName() + ".class)";
    }

    private static final long serialVersionUID = 0;
  }


}
