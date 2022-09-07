package eu.gaiax.difs.fc.core.service.verification.impl;

import com.danubetech.keyformats.JWK_to_PublicKey;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory;
import com.danubetech.keyformats.jose.JWK;
import com.danubetech.keyformats.keytypes.KeyTypeName_for_JWK;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import foundation.identity.did.DIDDocument;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link VerificationService} interface.
 */
@Service
public class VerificationServiceImpl implements VerificationService {
  private static final String credentials_key = "verifiableCredential";

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
    for (Map<String, Object> vc : credentials) {
      List<Map<String, Object>> credentialSubjects = (List<Map<String, Object>>) vc.get("credentialSubject");
      List<SdClaim> _claims = null; //TODO semantic verification and claim extraction
      claims.addAll(_claims);
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

  boolean verifyPEM(String uri) throws IOException {
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

    //TODO Check if x509 certificate is valid

    return true;
  }

  PublicKeyVerifier getVerifiedVerifier(LdProof proof) throws IOException {
    URI uri = proof.getVerificationMethod();
    String jwt = proof.getJws();
    JWK jwk;
    PublicKeyVerifier pubKey = null;
    String n;
    String x5u;

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

      n = (String) jwk_map_uncleaned.get("n");
      x5u = (String) jwk_map_uncleaned.get("x5u");
    } else throw new VerificationException("Unknown Verification Method: " + uri);

    if(!jwk.getN().equals(n)) throw new VerificationException("JWK does not match with provided certificate");

    if(!verifyPEM(x5u)) throw new VerificationException("JWK has no trust anchor in registry");

    return pubKey;
  }

  boolean checkCryptographic (VerifiablePresentation presentation) throws JsonLDException, GeneralSecurityException, IOException {
    LdProof proof = presentation.getLdProof();
    if (proof == null) throw new VerificationException("No proof found");

    PublicKeyVerifier publicKeyVerifier = getVerifiedVerifier(proof);
    LdVerifier verifier = new JsonWebSignature2020LdVerifier(publicKeyVerifier);

    if(!verifier.verify(presentation)) throw new VerificationException("VPs proof is not valid");

    //TODO Do we have to check the compliance credential too? YES!
    return true; //If this point was reached without an exception, the signature is valid
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
}
