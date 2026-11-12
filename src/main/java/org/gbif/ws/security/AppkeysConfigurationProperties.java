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
package org.gbif.ws.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "appkeys")
public class AppkeysConfigurationProperties {

  private String file;

  private List<String> whitelist;

  /**
   * Admin users each app key may act as through x-gbif-user, e.g. {@code gbif.occurrence.ws:
   * [download.gbif.org]}. Users with any *_ADMIN role count as admins.
   */
  private Map<String, List<String>> adminImpersonation = Collections.emptyMap();

  /** When false, an app acting as an admin not in {@link #adminImpersonation} is only logged. */
  private boolean enforceAdminImpersonation;

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public List<String> getWhitelist() {
    return whitelist;
  }

  public void setWhitelist(List<String> whitelist) {
    this.whitelist = whitelist;
  }

  public Map<String, List<String>> getAdminImpersonation() {
    return adminImpersonation;
  }

  public void setAdminImpersonation(Map<String, List<String>> adminImpersonation) {
    this.adminImpersonation =
        adminImpersonation != null ? adminImpersonation : Collections.emptyMap();
  }

  public boolean isEnforceAdminImpersonation() {
    return enforceAdminImpersonation;
  }

  public void setEnforceAdminImpersonation(boolean enforceAdminImpersonation) {
    this.enforceAdminImpersonation = enforceAdminImpersonation;
  }
}
