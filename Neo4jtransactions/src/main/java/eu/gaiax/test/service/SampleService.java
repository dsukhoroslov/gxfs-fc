package eu.gaiax.test.service;

import eu.gaiax.test.model.Address;
import eu.gaiax.test.repository.AddressRepositoryPostgres;
import eu.gaiax.test.model.Person;
import eu.gaiax.test.repository.PersonRepository;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class SampleService {

  /*@Autowired
  private PersonRepository personRepository;

  @Autowired
  private AddressRepositoryPostgres addressRepositoryPostgres;

  @Autowired
  private Driver driver;*/


  @Autowired
  private Neo4jClient neo4jClient;

  //private Neo4jRepository neo4jRepository;


  /*@Transactional
   public void testBothDBInsert(){

     Person test = new Person("Test");

     personRepository.save(test);

    Address address = new Address(1L,"Test Address");

     addressRepositoryPostgres.save(address);


    if (true){
     // throw new RuntimeException("test exception manually thrown ");
    }
  }*/

  public void testTemplate() {
    log.debug("testTemplate.enter;");
      
    String claimsAdded = "<http://example.org/Provider1_3> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "<http://example.org/Provider1_1> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "_:B643222240400c851edd9ad368de73998 <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "_:B643222240400c851edd9ad368de73998 <http://www.w3.org/2006/vcard/ns#street-address> \"Street Name\" .\n" +
        "_:B643222240400c851edd9ad368de73998 <http://www.w3.org/2006/vcard/ns#postal-code> \"1234\" .\n" +
        "_:B643222240400c851edd9ad368de73998 <http://www.w3.org/2006/vcard/ns#locality> \"Town Name\" .\n" +
        "_:B643222240400c851edd9ad368de73998 <http://www.w3.org/2006/vcard/ns#country-name> \"Country\" .\n" +
        "_:B643222240400c851edd9ad368de73998 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2006/vcard/ns#Address> .\n" +
        "<http://example.org/Provider1_6> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "_:Bbe3512eca3609b0b958bd82b929abbe3 <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "_:Bbe3512eca3609b0b958bd82b929abbe3 <https://w3id.org/gaia-x/gax-trust-framework#hash> \"1234\" .\n" +
        "_:Bbe3512eca3609b0b958bd82b929abbe3 <https://w3id.org/gaia-x/gax-trust-framework#content> \"http://example.org/tac\" .\n" +
        "_:Bbe3512eca3609b0b958bd82b929abbe3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/gaia-x/gax-trust-framework#TermsAndConditions> .\n" +
        "<http://example.org/Provider1_4> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#headquarterAddress> _:B643222240400c851edd9ad368de73998 .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#legalAddress> _:B84abdf5c0fd36c1676d1421d3e267ecd .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_5> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_1> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#registrationNumber> \"1234\" .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_4> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#legalName> \"Provider Name\" .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_7> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_3> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/gaia-x/gax-trust-framework#LegalPerson> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#termsAndConditions> _:Bbe3512eca3609b0b958bd82b929abbe3 .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_6> .\n" +
        "<https://w3id.org/gaia-x/core#Participant1> <https://w3id.org/gaia-x/gax-trust-framework#subOrganisation> <http://example.org/Provider1_2> .\n" +
        "_:B84abdf5c0fd36c1676d1421d3e267ecd <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "_:B84abdf5c0fd36c1676d1421d3e267ecd <http://www.w3.org/2006/vcard/ns#street-address> \"Street Name\" .\n" +
        "_:B84abdf5c0fd36c1676d1421d3e267ecd <http://www.w3.org/2006/vcard/ns#postal-code> \"1234\" .\n" +
        "_:B84abdf5c0fd36c1676d1421d3e267ecd <http://www.w3.org/2006/vcard/ns#locality> \"Town Name\" .\n" +
        "_:B84abdf5c0fd36c1676d1421d3e267ecd <http://www.w3.org/2006/vcard/ns#country-name> \"Country\" .\n" +
        "_:B84abdf5c0fd36c1676d1421d3e267ecd <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2006/vcard/ns#Address> .\n" +
        "<http://example.org/Provider1_2> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "<http://example.org/Provider1_7> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .\n" +
        "<http://example.org/Provider1_5> <http://w3id.org/gaia-x/service#claimsGraphUri> \"http://gaiax.de\" .";

    String query = "CALL n10s.rdf.import.inline($payload, \"N-Triples\")\n"
        + "YIELD terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo\n"
        + "RETURN terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo";

    Result rs = neo4jClient.getQueryRunner().run(query, Map.of("payload", claimsAdded));
    log.debug("testTemplate; results: {}", rs.consume());

    if (true) {
        throw new RuntimeException("test exception manually thrown ");
    }

  }

}
