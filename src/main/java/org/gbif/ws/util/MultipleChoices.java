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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A simple bean for a http 300 MultipleChoices response listing the available options.
 *
 * @see <a href="https://gbif.basecamphq.com/projects/7935093-portal/todo_items/107116544/comments">Basecamp</a>
 * @see <a href="http://tools.ietf.org/html/rfc2616#page-61">HTTP 1.1 specs</a>
 */
public class MultipleChoices {

  private final List<Choice> choices = new ArrayList<>();

  public List<Choice> getChoices() {
    return choices;
  }

  public void addChoice(Choice choice) {
    choices.add(choice);
  }

  public void addChoice(
      @Nullable Object key, String url, String title, @Nullable String description) {
    addChoice(new Choice(key, url, title, description));
  }
}
