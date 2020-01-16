package org.gbif.ws.util;

import javax.annotation.Nullable;

public class Choice {

  private final Object key;
  private final String url;
  private final String title;
  private final String description;

  public Choice(@Nullable Object key, String url, String title, @Nullable String description) {
    this.key = key;
    this.url = url;
    this.title = title;
    this.description = description;
  }

  public Object getKey() {
    return key;
  }

  public String getUrl() {
    return url;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }
}
