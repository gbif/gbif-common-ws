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
package org.gbif.ws.remoteauth.jwt;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class JwtUtils {

  private static final String BEARER_PREFIX = "Bearer ";

  /** Tries to find the token in the {@link HttpHeaders#AUTHORIZATION} header. */
  static Optional<String> findTokenInRequest(HttpServletRequest request) {
    // check header first
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .filter(JwtUtils::isBearer)
        .map(JwtUtils::removeBearer)
        .filter(StringUtils::isNotEmpty);
  }

  /**
   * Removes the "Bearer " scheme prefix and surrounding whitespace.
   *
   * @param header Authorization header starting with the Bearer scheme
   * @return the token
   */
  private static String removeBearer(String header) {
    return header.substring(BEARER_PREFIX.length()).trim();
  }

  /** The scheme name is case-insensitive (RFC 7235), only the Bearer scheme carries a JWT. */
  private static boolean isBearer(String header) {
    return StringUtils.startsWithIgnoreCase(header, BEARER_PREFIX);
  }
}
