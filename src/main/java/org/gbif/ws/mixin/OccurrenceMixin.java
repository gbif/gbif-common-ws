package org.gbif.ws.mixin;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.gbif.api.jackson.DateSerde;

import java.util.Date;

public interface OccurrenceMixin extends LicenseMixin {
  @JsonSerialize(using = DateSerde.NoTimezoneDateJsonSerializer.class)
  Date getDateIdentified();

  @JsonSerialize(using = DateSerde.NoTimezoneDateJsonSerializer.class)
  Date getEventDate();
}
