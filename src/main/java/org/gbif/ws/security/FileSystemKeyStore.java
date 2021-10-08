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

import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileSystemKeyStore implements KeyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemKeyStore.class);

  private final Map<String, String> store;

  public FileSystemKeyStore(AppkeysConfigurationProperties appkeysConfiguration) {
    try {
      Properties props = PropertiesUtil.loadProperties(appkeysConfiguration.getFile());
      Map<String, String> map = new HashMap<>();
      props.forEach((key, value) -> map.put(String.valueOf(key), String.valueOf(value)));
      store = Collections.unmodifiableMap(map);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Property file path to application keys does not exist: "
              + appkeysConfiguration.getFile(),
          e);
    }
    LOG.info("Initialised appkey store with {} keys", store.size());
  }

  @Override
  public String getPrivateKey(final String applicationKey) {
    return store.get(applicationKey);
  }
}
