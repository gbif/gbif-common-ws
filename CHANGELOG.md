# Change Log

## 2.1.4 (unreleased)

New GBIF request signature format, see [GBIF request signing](README.md#gbif-request-signing).
Signatures made by earlier versions are still accepted while `gbif.ws.security.gbifAuth.acceptLegacySignatures`
is `true` (the default), so servers can be deployed before clients are upgraded. Legacy signatures are
logged at WARN with their app key; set the flag to `false` once no more are logged. Clients built with this
version only produce the new format, so servers must be upgraded first.

 * Signatures use HMAC-SHA256 instead of HMAC-SHA1.
 * New signed `x-gbif-date` header; requests outside `gbif.ws.security.gbifAuth.maxClockSkewSeconds`
   (default 15 minutes) are rejected to limit replays ([informatics#52](https://github.com/gbif/informatics/issues/52)).
 * The canonical query string is now part of the signed URL.
 * `Content-MD5` is verified against the request body.
 * `x-url` and `x-method` must match the real request, except on `gbif.ws.security.gbifAuth.forwardedAuthPaths`
   (default `/user/auth/app`).
 * Signatures are compared in constant time ([informatics#56](https://github.com/gbif/informatics/issues/56)).
 * `commons-beanutils` upgraded to 1.11.0 for CVE-2025-48734 ([informatics#60](https://github.com/gbif/informatics/issues/60)).

## 0.40 (2017-08-07)

 * The class `org.gbif.ws.server.filter.AuthFilter` is now replaced by `IdentityFilter`.
 * `AppIdentityFilter` is now available to authenticate application.

[Complete Changelog](https://github.com/gbif/gbif-common-ws/compare/gbif-common-ws-0.39...gbif-common-ws-0.40)
