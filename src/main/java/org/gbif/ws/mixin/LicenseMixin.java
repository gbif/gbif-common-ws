package org.gbif.ws.mixin;

import org.gbif.api.jackson.LicenseSerde;
import org.gbif.api.vocabulary.License;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Mixin interface used to serialize license enums into urls.
 */
public interface LicenseMixin {

  @JsonSerialize(using = LicenseSerde.LicenseJsonSerializer.class)
  @JsonDeserialize(using = LicenseSerde.LicenseJsonDeserializer.class)
  License getLicense();
}
