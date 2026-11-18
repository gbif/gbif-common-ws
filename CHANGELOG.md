# Change Log

## 2.1.4 (unreleased)

**Breaking:** GBIF request signatures made by earlier versions are rejected. All signers, Java or not,
must be updated before servers are deployed with this version. See
[GBIF request signing](README.md#gbif-request-signing).

 * Signatures use HMAC-SHA256 instead of HMAC-SHA1.
 * New signed `x-gbif-date` header; requests outside `gbif.ws.security.gbifAuth.maxClockSkewSeconds`
   (default 15 minutes) are rejected to limit replays ([informatics#52](https://github.com/gbif/informatics/issues/52)).
 * The canonical query string is now part of the signed URL.
 * `Content-MD5` is verified against the request body. Form parameters of buffered
   `application/x-www-form-urlencoded` bodies are read from the buffer, as the servlet container can
   no longer read them, up to 10000 query plus form parameters (the default
   `server.tomcat.max-parameter-count`); more are rejected with 400. Multipart bodies are not
   buffered, so the container can parse their parts; their `Content-MD5` is not verified. Spring
   Boot's `FormContentFilter` reads PUT, PATCH and DELETE form bodies first, so services receiving
   signed form bodies with those methods must set `spring.mvc.formcontent.filter.enabled=false`.
 * `Content-MD5` is only accepted as the MD5 of the raw body, as the README describes. Java signers
   (`GbifAuthRequestInterceptor`, `GbifAuthServiceImpl#signRequest`) no longer hash the body serialized as a JSON
   byte array: accepting both let a body be replaced by its JSON-serialized form under the same signature.
   Constructors and `ClientBuilder#withCustomGbifAuth` taking an `Md5EncodeService` are deprecated, it is not used.
 * The signed path is the path as sent, percent-encoded, instead of the decoded path: Java clients could not sign
   requests to encoded paths such as `/dataset/a%20b`.
 * `x-url` and `x-method` must match the real request, except on `gbif.ws.security.gbifAuth.forwardedAuthPaths`
   (default `/user/auth/app`).
 * **Breaking:** the `x-url` path must be the request path, or one of `gbif.ws.security.gbifAuth.proxyPathPrefixes`
   (default `/v1`) followed by it, instead of any path ending with it; forwarded authentication paths are matched the
   same way. Services behind another proxy prefix (e.g. `/v1/directory`) must configure it
   ([security-issues#44](https://github.com/gbif/security-issues/issues/44)).
 * Signatures are compared in constant time ([informatics#56](https://github.com/gbif/informatics/issues/56)).
 * `commons-beanutils` upgraded to 1.11.0 for CVE-2025-48734 ([informatics#60](https://github.com/gbif/informatics/issues/60)).
 * **Breaking:** requests without credentials are no longer treated as authenticated
   ([informatics#53](https://github.com/gbif/informatics/issues/53)). `GbifAuthenticationToken.anonymous()`
   now returns a `GbifAnonymousAuthenticationToken` (a Spring `AnonymousAuthenticationToken` with
  `ROLE_ANONYMOUS`), so the authorization DSL's `.authenticated()` and method-security expressions
  `isAuthenticated()` / `isFullyAuthenticated()` reject anonymous callers. Direct calls to
  `Authentication.isAuthenticated()` still return `true` for anonymous tokens; consumers using that
  API must use `AuthenticationTrustResolver` or check for `AnonymousUserPrincipal` instead.
  `NoAuthWebSecurityConfigurer` now uses `permitAll()` at the URL level, as `.authenticated()`
  used to let anonymous requests through; endpoints stay secured by method security. Consumers that
  relied on anonymous passing method-security `isAuthenticated()` checks were open and must be reviewed.
 * `AppIdentityFilter` stops the filter chain after answering 401 to an invalid GBIF signature ([informatics#57](https://github.com/gbif/informatics/issues/57)).
 * Authorization headers, Basic credentials and app secrets are no longer written to logs or exception messages; an invalid Base64 Basic header is answered with 400 instead of 500 ([informatics#61](https://github.com/gbif/informatics/issues/61)).
 * `IdentityFilter` answers failed authentication as `text/plain; charset=UTF-8` without echoing the user name, and no longer catches exceptions thrown further down the filter chain ([informatics#62](https://github.com/gbif/informatics/issues/62)).
 * Removed the `@Retryable` from `AbstractRemoteAuthenticationProvider#tryLogin`; it had no effect and would have retried rejected credentials ([informatics#64](https://github.com/gbif/informatics/issues/64)).
 * JWTs are only read from an `Authorization` header using the `Bearer` scheme, instead of any `Authorization` header containing the word "bearer" anywhere in its value (security review L5, [infrastructure#66](https://github.com/gbif/infrastructure/issues/66)).
 * Search parameters are bounded ([informatics#58](https://github.com/gbif/informatics/issues/58)): `facetLimit` and `<facet>.facetLimit` are lowered to 1000 and `spellCheckCount` to 100; `facetOffset` and `<facet>.facetOffset` above 10000, negative values and non-numeric values are rejected with 400. The facet maxima can be changed with a new `FacetedSearchRequestProvider` constructor.
 * Buffered request bodies (`gbif.ws.security.wrapContent`, default true) are limited to `gbif.ws.security.maxBufferedBodyBytes` (default 10 MB, -1 for no limit); larger bodies get 413 ([informatics#54](https://github.com/gbif/informatics/issues/54)). The buffered body is kept as bytes, so `getContentBytes()` and `getInputStream()` return it exactly as received, and a missing or unsupported charset means UTF-8 instead of the platform default or a 500.
 * `XSSUtil` uses the OWASP Java HTML Sanitizer instead of regex patterns ([informatics#63](https://github.com/gbif/informatics/issues/63)), which closes nested-tag and event-handler bypasses and removes patterns that took quadratic time on input such as `<img` repeated. Changes for callers: `containsXSS` is true for any markup outside an allowlist (`b`, `strong`, `i`, `em`, `u`, `p`, `br`, `ul`, `ol`, `li`, `a` with an http(s) `href`), e.g. `x<y` is now flagged; `stripXSS` returns sanitized HTML, so its text is HTML encoded (`&` becomes `&amp;`). New dependency: `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1`, the version registry uses.
 * **Breaking:** 401 responses challenge with `WWW-Authenticate: GBIF realm="GBIF"` instead of `Basic`, so browsers no longer prompt for and keep Basic credentials that other sites could use ([informatics#59](https://github.com/gbif/informatics/issues/59)). Clients must send credentials up front; ones that wait for a Basic challenge, such as Apache HttpClient with only a `CredentialsProvider`, get 401. CSRF protection stays off: the API is stateless and has no cookies.
 * Apps acting as admin users through `x-gbif-user` must be listed in `appkeys.adminImpersonation` ([informatics#55](https://github.com/gbif/informatics/issues/55)). Pairs not listed are logged at WARN, and rejected with 403 once `appkeys.enforceAdminImpersonation` is true (default false). See [Acting as admin users](README.md#acting-as-admin-users).

## 0.40 (2017-08-07)

 * The class `org.gbif.ws.server.filter.AuthFilter` is now replaced by `IdentityFilter`.
 * `AppIdentityFilter` is now available to authenticate application.

[Complete Changelog](https://github.com/gbif/gbif-common-ws/compare/gbif-common-ws-0.39...gbif-common-ws-0.40)
