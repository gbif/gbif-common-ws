# gbif-common-ws

The gbif-common-ws shared library provides:
 * Base classes for GBIF Web Client accessing Web Service using [Feign](https://github.com/OpenFeign/feign)
 * Custom ObjectMapper provider [JacksonJsonObjectMapperProvider](src/main/java/org/gbif/ws/json/JacksonJsonObjectMapperProvider.java)
 * Security services and tools
 * Base classes for GBIF Web Services (server)
 * Some common utils and exceptions

## To build the project
```
mvn clean install
```

## GBIF request signing

Applications authenticate to GBIF web services by signing each request with their app key and secret.
Java clients get this from `GbifAuthRequestInterceptor` (Feign) or `GbifAuthService#signRequest`; other
clients must implement the steps below. The server side is `GbifAuthServiceImpl`.

Since 2.1.4 the scheme binds the date, query string and body to the signature. It is **not compatible**
with signatures made by older versions, see the [Change Log](CHANGELOG.md).

### Headers

| Header          | Value                                                                        |
|-----------------|------------------------------------------------------------------------------|
| `x-gbif-user`   | The user the application acts as                                             |
| `x-gbif-date`   | Current time in UTC, RFC 1123, e.g. `Wed, 11 Nov 2026 15:04:05 GMT`          |
| `Content-MD5`   | Base64 of the MD5 digest of the exact body bytes; omitted when there is no body |
| `Authorization` | `GBIF <appKey>:<signature>`                                                  |

### Building the signature

1. Canonicalize the URL: the request path followed, if the query string is not empty, by `?` and the
   canonical query string:
   - split the query on `&`, drop empty parameters, split each on the first `=` (no `=` means an empty value);
   - URL-decode names and values (`+` decodes to a space, so a literal `+` must be sent as `%2B`);
   - sort by name, then by value;
   - percent-encode names and values, leaving only `A-Z a-z 0-9 - _ . ~` unencoded (a space is `%20`, `*` is `%2A`);
   - join as `name=value` with `&`.
2. Build the string to sign by joining with `\n`:
   ```
   METHOD
   canonical URL
   content type, lowercased   (only if a Content-Type header is sent)
   Content-MD5                (only if a body is sent)
   x-gbif-user
   x-gbif-date
   ```
   The values must be exactly the ones sent in the headers.
3. Compute the HMAC-SHA256 of the string to sign (UTF-8) with the app secret and Base64-encode it.

Sign every attempt again, retries included, so the date is fresh.

### Example

App key `example.app`, secret `example-secret`, user `exampleuser`:

```
POST /v1/dataset?validate=true&tag=b*&tag=a+b
Content-Type: application/json

{"title":"Example dataset"}
```

String to sign:

```
POST
/v1/dataset?tag=a%20b&tag=b%2A&validate=true
application/json
BW6JIbyk1iXd2gEjg2Lo9Q==
exampleuser
Wed, 11 Nov 2026 15:04:05 GMT
```

Headers sent:

```
x-gbif-user: exampleuser
x-gbif-date: Wed, 11 Nov 2026 15:04:05 GMT
Content-MD5: BW6JIbyk1iXd2gEjg2Lo9Q==
Authorization: GBIF example.app:dxGpb0dP4c87l4xhXGLRZFB3Rs+MZXYKAsPYWAkJyAE=
```

### Server checks

A request is rejected with 401 when:
- `x-gbif-date` is missing, unparseable or more than `gbif.ws.security.gbifAuth.maxClockSkewSeconds`
  (default 900) away from the server clock;
- `Content-MD5` does not match the body (checked when the request content is wrapped, `gbif.ws.security.wrapContent`,
  default true). Buffered bodies larger than `gbif.ws.security.maxBufferedBodyBytes` (default 10 MB, -1 for no limit)
  are rejected with 413;
- `x-method` differs from the request method, or `x-url` does not refer to the request path. These headers are
  only taken as-is on `gbif.ws.security.gbifAuth.forwardedAuthPaths` (default `/user/auth/app`), used by
  remote authentication;
- the signature does not match.

### Acting as admin users

A signed request acts as the user in `x-gbif-user`. When that user has an admin role (any `*_ADMIN` role, e.g.
`REGISTRY_ADMIN`), the app key must be allowed to act as that user:

```yaml
appkeys:
  adminImpersonation:
    gbif.occurrence.ws: [download.gbif.org]
  # false (default): pairs not listed are only logged at WARN; true: they are rejected with 403
  enforceAdminImpersonation: false
```

Every app acting as an admin is logged at WARN with the app key and user. Run with enforcement off, check the
logs for pairs to add, then set `enforceAdminImpersonation: true`.

## Change Log
[Change Log](CHANGELOG.md)

## Documentation
[JavaDocs](http://gbif.github.io/gbif-common-ws/apidocs/)
