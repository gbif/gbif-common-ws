package org.gbif.ws.remoteauth;

import org.gbif.api.model.common.GbifUser;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserAdmin {

  private GbifUser user;
  private boolean challengeCodePresent;

}
