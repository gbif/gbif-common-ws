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
package org.gbif.ws.security.identity.model;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Builder;
import lombok.Data;

/** Class top map responses from token-based (JWT) authentication services.*/
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = LoggedUser.LoggedUserBuilder.class)
public class LoggedUser {

  private Integer key;
  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private String token;

  @Builder.Default private Map<String, String> settings = new HashMap<>();

  @Builder.Default private Set<String> roles = new HashSet<>();

  public GbifUser toGbifUser() {
    GbifUser gbifUser = new GbifUser();
    gbifUser.setUserName(userName);
    gbifUser.setFirstName(firstName);
    gbifUser.setLastName(lastName);
    gbifUser.setEmail(email);
    gbifUser.setSettings(settings);
    gbifUser.setKey(key);
    Optional.ofNullable(roles).map(r -> roles.stream().map(UserRole::valueOf).collect(Collectors.toSet()))
      .ifPresent(gbifUser::setRoles);
    return gbifUser;
  }

}
