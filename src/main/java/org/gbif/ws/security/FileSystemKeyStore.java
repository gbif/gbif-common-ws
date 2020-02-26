package org.gbif.ws.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Properties;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileSystemKeyStore implements KeyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemKeyStore.class);

  private final ImmutableMap<String, String> store;

  public FileSystemKeyStore(AppkeysConfiguration appkeysConfiguration) {
    try {
      Properties props = PropertiesUtil.loadProperties(appkeysConfiguration.getFile());
      store = Maps.fromProperties(props);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Property file path to application keys does not exist: "
              + appkeysConfiguration.getFile(),
          e);
    }
    LOG.info("Initialised appkey store with {} keys", store.size());
  }

  public String getPrivateKey(final String applicationKey) {
    return store.get(applicationKey);
  }
}
