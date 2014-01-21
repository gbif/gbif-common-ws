/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.gbif.ws.client.filter;

import org.gbif.ws.security.GbifAppAuthService;

import java.security.Principal;

import com.google.inject.Provider;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Thread safe client filter adding an HTTP Authentication header to the HTTP request using the custom GBIF schema
 * for trusted applications.
 * In addition to the Authentication this filter will add these headers to the request:
 * <ul>
 * <li>Content-MD5: the MD5 hash for the request body</li>
 * <li>x-gbif-user: the username of the proxied user</li>
 * <li>x-gbif-date: the current datetime stamp</li>
 * </ul>
 * The custom date header is added in addition to the regular date header because it is not possible
 * to access the generated date header in a jersey ClientFilter. The regular date header is generated by
 * the RequestDate HttpRequestInterceptor of HttpClient and is therefore not available to jersey client.
 * A current timestamp is important for signing the request though.
 */
public final class HttpGbifAuthFilter extends ClientFilter {

  private final String applicationId;
  private final Provider<Principal> principalProvider;
  private final GbifAppAuthService authService;

  /**
   * Creates a new HTTP GBIF Authentication filter using provided application ID and trusted principal provider.
   */
  public HttpGbifAuthFilter(final String applicationId, GbifAppAuthService authService,
    Provider<Principal> principalProvider) {
    this.applicationId = applicationId;
    this.principalProvider = principalProvider;
    this.authService = authService;
  }

  @Override
  public ClientResponse handle(final ClientRequest cr) throws ClientHandlerException {

    if (principalProvider != null) {
      Principal prince = principalProvider.get();
      if (prince != null) {
        authService.signRequest(applicationId, prince.getName(), cr);
      }
    }
    return getNext().handle(cr);
  }

}
