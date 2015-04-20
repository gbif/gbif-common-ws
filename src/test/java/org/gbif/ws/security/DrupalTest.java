package org.gbif.ws.security;

import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.ws.json.JacksonJsonContextResolver;

import java.net.URI;
import java.util.UUID;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.client.impl.ClientRequestImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;

@Ignore("Manually testing drupal calls to verify the gbif auth scheme in php")
public class DrupalTest {

  public static void main(String[] args) throws Exception {
    Client client = new Client();

    GbifAuthService auth = GbifAuthService.singleKeyAuthService("gbif.drupal", "xxx");
    ObjectMapper mapper = new JacksonJsonContextResolver().getContext(null);

    Organization org = new Organization();
    org.setTitle("title");
    org.setCountry(Country.AFGHANISTAN);
    org.setDescription("descript");
    org.setEndorsingNodeKey(UUID.fromString("4ddd294f-02b7-4359-ac33-0806a9ca9c6b"));
    String data = mapper.writeValueAsString(org);

    URI uri = URI.create("http://api.gbif-dev.org/v1/organization");
    ClientRequest req = new ClientRequestImpl(uri, "POST", data);
    req.getHeaders().putSingle(GbifAuthService.HEADER_CONTENT_TYPE, MediaType.APPLICATION_JSON);
    //req.getHeaders().putSingle(GbifAuthService.HEADER_CONTENT_TYPE, "application/json; charset=UTF-8");
    auth.signRequest("bko", req);


    ClientResponse resp = client.handle(req);
    System.out.print(resp);
  }

}
