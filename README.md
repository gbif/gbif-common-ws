# gbif-common-ws

The gbif-common-ws shared library provides:
 * [Jersey](https://jersey.java.net/) 1 custom [Providers](https://jersey.java.net/documentation/latest/message-body-workers.html), [Filters](https://jersey.java.net/documentation/latest/filters-and-interceptors.html) and [ExceptionMapper](https://jersey.java.net/nonav/apidocs/1.18/jersey/javax/ws/rs/ext/ExceptionMapper.html)
 * Customized [JacksonJsonContextResolver](https://github.com/gbif/gbif-common-ws/blob/master/src/main/java/org/gbif/ws/json/JacksonJsonContextResolver.java)
 * Base for Client accessing Web Service using Jersey [WebResource](https://jersey.java.net/nonav/apidocs/1.19/jersey/com/sun/jersey/api/client/WebResource.html)
 * Guice modules for Web Service server and clients

## To build the project
```
mvn clean install
```

## Limitation
This library can not be easily updated to use Jersey 2.
As stated in the [Jersey documentation](https://jersey.java.net/nonav/documentation/2.0/migration.html): " ... there are many incompatiblities between Jersey 1.x and Jersey 2.0"