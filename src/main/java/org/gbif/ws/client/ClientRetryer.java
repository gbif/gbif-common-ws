package org.gbif.ws.client;

import feign.RetryableException;
import feign.Retryer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ClientRetryer implements Retryer {

  private static final Logger LOG = LoggerFactory.getLogger(ClientRetryer.class);

  private final int maxAttempts;
  private final long period;
  private final double multiplier;
  int attempt;
  long sleptForMillis;

  public ClientRetryer() {
    this(1000, 3, 1.5);
  }

  public ClientRetryer(long period, int maxAttempts, double multiplier) {
    this.period = period;
    this.maxAttempts = maxAttempts;
    this.multiplier = multiplier;
    this.attempt = 1;
  }

  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public void continueOrPropagate(RetryableException e) {
    if (attempt++ >= maxAttempts) {
      LOG.error("All {} retry attempts failed. Giving up. Last execution was: '{}: {}'",
          maxAttempts, e.getClass().getSimpleName(), e.getMessage());
      throw e;
    }

    long interval;
    if (e.retryAfter() != null) {
      interval = e.retryAfter().getTime() - currentTimeMillis();
      if (interval < 0) {
        return;
      }
    } else {
      interval = nextMaxInterval();
    }
    try {
      Thread.sleep(interval);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      throw e;
    }
    sleptForMillis += interval;
  }

  /**
   * Calculates the time interval to a retry attempt. <br>
   * The interval increases exponentially with each attempt, at a rate of nextInterval *= multiplier
   * (where multiplier is the backoff factor), to the maximum interval.
   *
   * @return time in nanoseconds from now until the next attempt.
   */
  long nextMaxInterval() {
    return (long) (period * Math.pow(multiplier, attempt - 1));
  }

  @Override
  public Retryer clone() {
    return new ClientRetryer(period, maxAttempts, multiplier);
  }
}
