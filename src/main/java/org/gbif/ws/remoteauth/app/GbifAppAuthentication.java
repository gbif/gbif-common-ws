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
package org.gbif.ws.remoteauth.app;

import org.gbif.ws.security.GbifAuthUtils;

import java.util.Objects;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * GBIF APP {@link org.springframework.security.core.Authentication}.
 */
public class GbifAppAuthentication extends AbstractAuthenticationToken {

  private String gbifScheme;
  private String gbifUser;
  private String contentMd5;
  private String originalRequestUrl;

  public GbifAppAuthentication(String gbifScheme, String gbifUser, String contentMd5, String originalRequestUrl) {
    super(null);
    this.gbifScheme = gbifScheme;
    this.gbifUser = gbifUser;
    this.contentMd5 = contentMd5;
    this.originalRequestUrl = originalRequestUrl;
    super.setAuthenticated(false);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return gbifUser;
  }

  public String getAppKey() {
    return GbifAuthUtils.getAppKeyFromRequest(gbifScheme);
  }

  public String getGbifScheme() {
    return gbifScheme;
  }

  public String getContentMd5() {
    return contentMd5;
  }

  public String getOriginalRequestUrl() {
    return originalRequestUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    GbifAppAuthentication that = (GbifAppAuthentication) o;
    return Objects.equals(gbifScheme, that.gbifScheme) && Objects.equals(gbifUser, that.gbifUser)
        && Objects.equals(contentMd5, that.contentMd5) && Objects.equals(originalRequestUrl,
        that.originalRequestUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), gbifScheme, gbifUser, contentMd5, originalRequestUrl);
  }
}
