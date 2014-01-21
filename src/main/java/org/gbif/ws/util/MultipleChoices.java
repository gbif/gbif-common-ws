package org.gbif.ws.util;

import java.util.List;
import javax.annotation.Nullable;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A simple bean for a http 300 MultipleChoices response listing the available options.
 *
 * @see <a href="https://gbif.basecamphq.com/projects/7935093-portal/todo_items/107116544/comments">Basecamp</a>
 * @see <a href="http://tools.ietf.org/html/rfc2616#page-61">HTTP 1.1 specs</a>
 */
public class MultipleChoices {

  private final List<Choice> choices = newArrayList();

  public List<Choice> getChoices() {
    return choices;
  }

  public void addChoice(Choice choice) {
    choices.add(choice);
  }

  public void addChoice(@Nullable Object key, String url, String title, @Nullable String description) {
    addChoice(new Choice(key, url, title, description));
  }

}
