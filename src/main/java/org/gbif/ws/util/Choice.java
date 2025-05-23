/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ws.util;

import jakarta.annotation.Nullable;

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
