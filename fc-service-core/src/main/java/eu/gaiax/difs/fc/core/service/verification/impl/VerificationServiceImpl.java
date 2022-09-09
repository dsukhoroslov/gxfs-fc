package eu.gaiax.difs.fc.core.service.verification.impl;

import com.danubetech.keyformats.JWK_to_PublicKey;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory;
import com.danubetech.keyformats.jose.JWK;
import com.danubetech.keyformats.keytypes.KeyTypeName_for_JWK;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import foundation.identity.did.DIDDocument;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
import liquibase.pro.packaged.O;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;


// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link VerificationService} interface.
 */
@Service
public class VerificationServiceImpl implements VerificationService {
  private static final String credentials_key = "verifiableCredential";
  private static final String sd_format = "JSONLD";
  private static final String shapes_format = "TURTLE";
  private static final Logger logger = LoggerFactory.getLogger(VerificationServiceImpl.class);
  private static final Marker MARKER = MarkerFactory.getMarker("MARKER");

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResultParticipant verifyParticipantSelfDescription(ContentAccessor payload) throws VerificationException {
    VerifiablePresentation presentation = parseSD(payload);
    if(!isSDParticipant(presentation)) {
      String msg = "Expected Participant SD, got: ";

      if(isSDServiceOffering(presentation)) msg += "Serivce Offering SD";
      else msg += "Unknown SD";

      throw new VerificationException(msg);
    }
    return verifyParticipantSelfDescription(presentation);
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResultOffering verifyOfferingSelfDescription(ContentAccessor payload) throws VerificationException {
    VerifiablePresentation presentation = parseSD(payload);
    if(!isSDServiceOffering(presentation)) {
      String msg = "Expected Service Offering SD, got: ";

      if(isSDParticipant(presentation)) msg += "Participant SD";
      else msg += "Unknown SD";

      throw new VerificationException(msg);
    }
    return verifyOfferingSelfDescription(presentation);
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(ContentAccessor payload) throws VerificationException {
    VerifiablePresentation presentation = parseSD(payload);

    if(isSDParticipant(presentation)) {
      return verifyParticipantSelfDescription(presentation);
    } else if (isSDServiceOffering(presentation)) {
      return verifyOfferingSelfDescription(presentation);
    } else {
      throw new VerificationException("SD is neither a Participant SD nor a ServiceOffer SD");
    }
  }

  /*package private functions*/

  VerificationResultParticipant verifyParticipantSelfDescription(VerifiablePresentation presentation) throws VerificationException {
    if(!isSDParticipant(presentation)) throw new VerificationException("Expected a participant");

    try {
      checkCryptographic(presentation);
    } catch (JsonLDException | GeneralSecurityException | IOException e) {
      throw new VerificationException(e);
    }

    String id = getParticipantID(presentation); //parameter validator

    Map<String, Object> sd = cleanSD(presentation);
    List<Map<String, Object>> credentials = (List<Map<String, Object>>) sd.get("verifiableCredential");
    List<SdClaim> claims = new ArrayList<>();
    SchemaStore schemaStore = new SchemaStoreImpl();
    ContentAccessor shaclShapeCompositeSchema = schemaStore.getCompositeSchema(SchemaStore.SchemaType.SHAPE);
    String validationReport = validationAgainstShacl(presentation,shaclShapeCompositeSchema).getValidationReport();
    if (validationAgainstShacl(presentation,shaclShapeCompositeSchema).isConforming()) {
      for (Map<String, Object> vc : credentials) {
        List<Map<String, Object>> credentialSubjects = (List<Map<String, Object>>) vc.get("credentialSubject");
        List<SdClaim> _claims = extractClaims(credentialSubjects); //TODO semantic verification and claim extraction
        claims.addAll(_claims);
      }
    } else {
      throw new VerificationException("the self description is violating the shacl shape schema : "+validationReport);
    }

    return new VerificationResultParticipant(
            "name",
            id,
            presentation.getLdProof().getVerificationMethod().toString(),
            OffsetDateTime.now(),
            "lifecycle", //TODO Where to get this
            LocalDate.MIN,
            new ArrayList<>(),
            claims
    );
  }

  @Override
  public boolean checkValidator(Validator validator) {
    if (validator.getExpirationDate().isBefore(Instant.now())) return false;
    //check if pubkey is the same
    //check if pubkey is trusted
    return true; //if all checks succeeded the validator is valid
  }

  /*package private functions*/
  VerificationResultOffering verifyOfferingSelfDescription(VerifiablePresentation presentation) throws VerificationException {
    if(!isSDServiceOffering(presentation)) throw new VerificationException("Expected a service offering");

    try {
      checkCryptographic(presentation);
    } catch (JsonLDException | GeneralSecurityException | IOException e) {
      throw new VerificationException(e);
    }

    String issuer = getIssuer(presentation);

    Map<String, Object> sd = cleanSD(presentation);
    List<Map<String, Object>> credentials = (List<Map<String, Object>>) sd.get("verifiableCredential");
    List<SdClaim> claims = new ArrayList<>();

    for (Map<String, Object> vc : credentials) {
      List<Map<String, Object>> credentialSubjects = (List<Map<String, Object>>) vc.get("credentialSubject");
      List<SdClaim> _claims = null; //TODO semantic verification and claim extraction
      claims.addAll(_claims);
    }

    return new VerificationResultOffering(
            presentation.getId().toString(),
            issuer,
            OffsetDateTime.now(),
            "", //TODO where to get this?
            LocalDate.parse((String) sd.get("issuanceDate")),
            null,
            claims
    );
  }

  VerifiablePresentation parseSD(ContentAccessor accessor) {
    try {
      return VerifiablePresentation.fromJson(accessor.getContentAsString()
              .replaceAll("JsonWebKey2020", "JsonWebSignature2020"));
      //This has to be done to handle current examples. In the final code the replacement becomes obsolete
      //TODO remove replace
    } catch (RuntimeException e) {
      throw new VerificationException(e);
    }
  }

  Map<String, Boolean> getSDType (VerifiablePresentation presentation) {
    boolean isParticipant = false;
    boolean isServiceOffering = false;

    List<String> types = (List<String>) presentation.getJsonObject().get("@type");

    for (String type:types) {
      if(type.contains("LegalPerson")) {
        isParticipant = true;
      }
      if(type.contains("ServiceOfferingExperimental")) { //TODO is this type final? No Credential subject type
        isServiceOffering = true;
      }
    }

    if (isParticipant && isServiceOffering) {
      throw new VerificationException("SD is both, a participant and an offering SD");
    }

    boolean finalIsParticipant = isParticipant;
    boolean finalIsServiceOffering = isServiceOffering;

    return new HashMap<>() {{
      put("participant", finalIsParticipant);
      put("offering", finalIsServiceOffering);
    }};
  }

  boolean isSDServiceOffering (VerifiablePresentation presentation) {
    return getSDType(presentation).get("offering").booleanValue();
  }

  boolean isSDParticipant (VerifiablePresentation presentation) {
    return getSDType(presentation).get("participant").booleanValue();
  }

  //TODO This function becomes obsolete when a did resolver will be available
  //https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/13
  public static DIDDocument readDIDfromURI (URI uri) throws IOException {
    String [] uri_parts = uri.getSchemeSpecificPart().split(":");
    String did_json;
    if(uri_parts[0].equals("web")) {
      String [] _parts = uri_parts[1].split("#");
      URL url;
      if (_parts.length == 1) {
        url = new URL("https://" + _parts[0] +"/.well-known/did.json");
      } else {
        url = new URL("https://" + _parts[0] +"/.well-known/did.json" + _parts[1]);
      }
      InputStream stream = url.openStream();
      did_json = IOUtils.toString(stream, StandardCharsets.UTF_8);
    } else {
      throw new RuntimeException("Couldn't load key. Origin not supported");
    }
    return DIDDocument.fromJson(did_json);
  }

  Map<String, Object> extractRelevantVerificationMethod (List<Map<String, Object>> methods, URI verificationMethodURI) {
    return methods.get(0);
    //TODO wait for answer https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/22
  }

  Map<String, Object> extractRelevantValues (Map<String, Object> map) {
    Map<String, Object> new_map = new HashMap<>();
    String [] relevants = {"kty", "d", "e", "kid", "use", "x", "y", "n", "crv"};
    for (String relevant:relevants) {
      if (map.containsKey(relevant)) {
        new_map.put(relevant, map.get(relevant));
      }
    }
    return new_map;
  }

  String getAlgorithmFromJWT(String jwt) {
    String[] chunks = jwt.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();

    String header = new String(decoder.decode(chunks[0]));
    try {
      return (String) JsonLDObject.fromJson(header).getJsonObject().get("alg");
    } catch (RuntimeException ex) {
      throw new VerificationException(ex);
    }
  }

  Validator getValidatorFromPEM(String uri, String did) throws IOException {
    //Is the PEM anchor in the registry?
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = new HttpPost("https://registry.lab.gaia-x.eu/api/v2204/trustAnchor/chain/file");

    // Request parameters and other properties.
    List<NameValuePair> params = new ArrayList<NameValuePair>(1);
    params.add(new BasicNameValuePair("uri", uri));
    httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    //Execute and get the response.
    HttpResponse response = httpclient.execute(httppost);
    if(response.getStatusLine().getStatusCode() != 200) throw new VerificationException("The trust anchor is not set in the registry");

    //TODO
    //https://www.baeldung.com/java-read-pem-file-keys
    //https://www.bouncycastle.org/docs/docs1.5on/index.html
    //https://stackoverflow.com/questions/12967016/how-to-check-ssl-certificate-expiration-date-programmatically-in-java
    
    return new Validator(
            did,
            "",
            null
    );
  }

  Object [] getVerifiedVerifier(LdProof proof) throws IOException {
    URI uri = proof.getVerificationMethod();
    String jwt = proof.getJws();
    JWK jwk;
    PublicKeyVerifier pubKey;
    Validator validator;

    if (!proof.getType().equals("JsonWebSignature2020")) throw new UnsupportedOperationException("This proof type is not yet implemented");

    if (uri.getScheme().equals("did")) {
      DIDDocument did = readDIDfromURI(uri);

      List<Map<String, Object>> available_jwks = (List<Map<String, Object>>) did.toMap().get("verificationMethod");
      Map<String, Object> jwk_map_uncleaned = (Map<String, Object>) extractRelevantVerificationMethod(available_jwks, uri).get("publicKeyJwk");
      Map<String, Object> jwk_map_cleaned = extractRelevantValues(jwk_map_uncleaned);

      // use from map and extract only relevant
      jwk = JWK.fromMap(jwk_map_cleaned);

      try {
        pubKey = PublicKeyVerifierFactory.publicKeyVerifierForKey(
                KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk),
                getAlgorithmFromJWT(jwt),
                JWK_to_PublicKey.JWK_to_anyPublicKey(jwk));
      } catch (IllegalArgumentException ex) {
        throw new VerificationException(ex);
      }

      validator = getValidatorFromPEM((String) jwk_map_uncleaned.get("x5u"), uri.toString());

      if(!jwk.getN().equals(validator.getPublicKey()))
        throw new VerificationException("JWK does not match with provided certificate");
    } else throw new VerificationException("Unknown Verification Method: " + uri);

    return new Object[]{pubKey, validator};
  }

  Validator checkCryptographic (JsonLDObject payload, LdProof proof) throws JsonLDException, GeneralSecurityException, IOException {
    if (proof == null) throw new VerificationException("No proof found");
    LdVerifier verifier;
    Validator validator = null; //TODO Cache.getValidator(proof.getVerificationMethod().toString());
    if (validator == null) {
      Object [] pkVerifierAndValidator = getVerifiedVerifier(proof);
      PublicKeyVerifier publicKeyVerifier = (PublicKeyVerifier) pkVerifierAndValidator[0];
      validator = (Validator) pkVerifierAndValidator[1];
      verifier = new JsonWebSignature2020LdVerifier(publicKeyVerifier);
    } else {
      verifier = getVerifierFromValidator(validator);
    }
    if(!verifier.verify(payload)) throw new VerificationException(payload.getClass().getName() + "does not matcht with proof");

    return validator;
  }

  private LdVerifier getVerifierFromValidator(Validator validator) {
    //TODO
    return null;
  }

  List<Validator> checkCryptographic (VerifiablePresentation presentation) throws JsonLDException, GeneralSecurityException, IOException {
    List<Validator> validators = new ArrayList<>();

    validators.add(checkCryptographic(presentation, presentation.getLdProof()));

    List<Map<String, Object>> credentials = (List<Map<String, Object>>) presentation.getJsonObject().get("verifiableCredential");
    for (Map<String, Object> _credential : credentials) {
      VerifiableCredential credential = VerifiableCredential.fromMap(_credential);
      validators.add(checkCryptographic(credential, credential.getLdProof()));
    }

    //If this point was reached without an exception, the signatures are valid
    return validators;
  }

  String getParticipantID(VerifiablePresentation presentation) {
    //TODO compare to validators
    return presentation.getVerifiableCredential().getId().toString();
  }

  String getIssuer(VerifiablePresentation presentation) {
    //TODO compare to validators
    //
    CredentialSubject credentialSubject = presentation.getVerifiableCredential().getCredentialSubject();
    String [] subjects = credentialSubject.getClaims().keySet().toArray(new String[0]);
    for (String subject:subjects) {
      if (subject.contains("providedBy")) {
        return (String) credentialSubject.getClaims().get(subject);
      }
    }
    throw new VerificationException("Provided By was not specified");
  }

  Map<String, Object> cleanSD (VerifiablePresentation presentation) {
    Map<String, Object> sd = presentation.getJsonObject();

    sd.remove("proof");
    ArrayList<Map<String, Object>> credentials = (ArrayList<Map<String, Object>>) sd.get(credentials_key);
    for (Map<String, Object> credential : credentials) {
      credential.remove("proof");
    }

    return sd;
  }

  /**
   * Method that validates a dataGraph against shaclShape
   *
   * @param sd   ContentAccessor of a self-Description sd to be validated
   * @param shaclShape ContentAccessor of a union schemas of type SHACL
   * @return                SemanticValidationResult object
   */
  SemanticValidationResult validationAgainstShacl(VerifiablePresentation sd, ContentAccessor shaclShape) {
    String validationReport = "";
    boolean conforms = false;
    try {
      Reader dataGraphReader = new StringReader(sd.toJson());
      Reader shaclShapeReader = new StringReader(shaclShape.getContentAsString());
      Model data = ModelFactory.createDefaultModel();
      data.read(dataGraphReader, null, sd_format);
      Model shape = ModelFactory.createDefaultModel();
      shape.read(shaclShapeReader, null, shapes_format);
      Resource reportResource = ValidationUtil.validateModel(data, shape, true);
      conforms = reportResource.getProperty(SH.conforms).getBoolean();
      logger.trace("Conforms = " + conforms);

      if (!conforms) {
        validationReport = reportResource.getModel().toString();
      }

    } catch (Throwable t) {
      logger.error(MARKER, t.getMessage(), t);
    }
    return new SemanticValidationResult(
            conforms,
            validationReport
    );
  }
  /**
   * A method that returns a list of claims given a self-description as map
   *
   * @param sd map represents a self-description for claims extraction
   * @return a list of claims.
   */
  List<SdClaim> extractClaims( List<Map<String, Object>> sd) {

    List<SdClaim> sdClaims = new ArrayList<>();
   // Map<String, Object> subjects = (Map<String, Object>) sd.get(credential_subject);
    String subject = sd.get("id").toString();
    Map<String, Map<String, Object>> credentialSubject = (Map<String, Map<String, Object>>) sd.get("credentialSubject");
    Set<String> credentialSubjectKeySets = credentialSubject.keySet();
    credentialSubjectKeySets.remove("@context");
    credentialSubjectKeySets.remove("id");
    String object = "";
    for (String predicate : credentialSubjectKeySets) {
      Map<String, Object> m1 = credentialSubject.get(predicate);
      for (String k1 : m1.keySet()) {
        if (!k1.contains("@type")) {
          object = m1.get(k1).toString();
          if(object.contains("{")) {
            LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>)m1.get(k1);
            object = map.get("@value").toString();
            sdClaims.add(new SdClaim(subject,predicate,object));

          } else  {
            sdClaims.add(new SdClaim(subject,predicate,object));
          }

        }
      }
    }
    return sdClaims;
  }


}
