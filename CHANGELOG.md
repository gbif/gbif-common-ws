# Change Log

## 2.1.4 (unreleased)

**Breaking:** GBIF request signatures made by earlier versions are rejected. All signers, Java or not,
must be updated before servers are deployed with this version. See
[GBIF request signing](README.md#gbif-request-signing).

 * Signatures use HMAC-SHA256 instead of HMAC-SHA1.
 * New signed `x-gbif-date` header; requests outside `gbif.ws.security.gbifAuth.maxClockSkewSeconds`
   (default 15 minutes) are rejected to limit replays ([informatics#52](https://github.com/gbif/informatics/issues/52)).
 * The canonical query string is now part of the signed URL.
 * `Content-MD5` is verified against the request body.
 * `x-url` and `x-method` must match the real request, except on `gbif.ws.security.gbifAuth.forwardedAuthPaths`
   (default `/user/auth/app`).
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
 * JWTs are only read from an `Authorization` header using the `Bearer` scheme, instead of any header containing the word "bearer" (security review L5, [infrastructure#66](https://github.com/gbif/infrastructure/issues/66)).

## 0.40 (2017-08-07)

 * The class `org.gbif.ws.server.filter.AuthFilter` is now replaced by `IdentityFilter`.
 * `AppIdentityFilter` is now available to authenticate application.

[Complete Changelog](https://github.com/gbif/gbif-common-ws/compare/gbif-common-ws-0.39...gbif-common-ws-0.40)
