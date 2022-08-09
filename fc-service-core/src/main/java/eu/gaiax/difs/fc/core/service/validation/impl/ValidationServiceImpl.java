package eu.gaiax.difs.fc.core.service.validation.impl;

import com.github.jsonldjava.utils.JsonUtils;
import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.core.exception.AccessDeniedException;
import eu.gaiax.difs.fc.core.exception.ValidationException;
import eu.gaiax.difs.fc.core.pojo.Claim;
import eu.gaiax.difs.fc.core.pojo.Signature;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.validation.ValidationService;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link ValidationService} interface.
 */
@Service
public class ValidationServiceImpl implements ValidationService {
  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public Participant validateParticipantSelfDescription(String json) throws ValidationException {
    return null;
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(String json) throws ValidationException, AccessDeniedException {
    boolean verifyOffering = true;
    String id = "";
    String issuer = "";
    String verificationTimestamp = "";
    String lifecycleStatus = "";
    String participantName = "";
    String participantPublicKey = "";
    Instant issuedDate = Instant.now();
    List<Signature> signatures = null;
    List<Claim> claims = null;

    //Verify Syntax and parse json
    Map<String, Object> parsedSD = parseSD(json);

    //TODO: Verify Cryptographic FIT-WI
    signatures = validateCryptographic(parsedSD);

    //TODO: Check if API-User is allowed to submit the self-description FIT-WI

    parsedSD = cleanSD(parsedSD);

    //TODO: Verify Schema FIT-DSAI

    //TODO: Extract Claims FIT-DSAI

    //Decide what to return
    if (verifyOffering) {
      return new VerificationResultOffering(
              id,
              issuer,
              verificationTimestamp,
              lifecycleStatus,
              issuedDate,
              signatures,
              claims
      );
    } else {
      return new VerificationResultParticipant(
              participantName,
              id,
              participantPublicKey,
              verificationTimestamp,
              lifecycleStatus,
              issuedDate,
              signatures,
              claims
      );
    }
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public SelfDescription validateSelfDescription(String json) throws ValidationException {
    SelfDescription sdMetadata = new SelfDescription();
    sdMetadata.setId("string");
    sdMetadata.setSdHash("string");
    sdMetadata.setIssuer("http://example.org/test-provider");
    sdMetadata.setStatus(SelfDescription.StatusEnum.ACTIVE);
    List<String> validators = new ArrayList<>();
    validators.add("string");
    sdMetadata.setValidators(validators);
    sdMetadata.setStatusTime("2022-05-11T15:30:00Z");
    sdMetadata.setUploadTime("2022-03-01T13:00:00Z");
    return sdMetadata;
  }

  /*package private functions*/

  Map<String, Object> parseSD (String json) throws ValidationException {
    Object parsed;
    try {
      parsed = JsonUtils.fromString(json);
    } catch (IOException e) {
      throw new ValidationException(e.getMessage());
    }

    return (Map<String, Object>) parsed;
  }

  List<Signature> validateCryptographic (Map<String, Object> sd) throws AccessDeniedException {
    List<Signature> signatures = new ArrayList<>();
    //Validate VP's signature
    hasSignature(sd);


    //For Each VC: Validate VC's signature
    List<Map<String, Object>> credentials = (List<Map<String, Object>>) sd.get("verifiableCredential");
    for (Map<String, Object> credential: credentials) {
      hasSignature(credential);
    }

    return signatures;
  }

  Map<String, Object> cleanSD (Map<String, Object> sd) {
    String credentials_key = "verifiableCredential";

    //TODO remove proofs
    sd.remove("proof");
    ArrayList<Map<String, Object>> credentials = (ArrayList<Map<String, Object>>) sd.get(credentials_key);
    for (Map<String, Object> credential : credentials) {
      credential.remove("proof");
    }
    sd.replace(credentials_key, credentials);

    return sd;
  }

  void hasSignature (Map<String, Object> cred) {
    if (cred == null || cred.isEmpty()) {
      throw new ValidationException("the credential is empty");
    }

    if(! cred.containsKey("proof")) {
      throw new ValidationException("no proof found");
    }

    Map<String,Object>  proofLHM = (Map<String,Object> ) cred.get("proof");
    if(! proofLHM.containsKey("type") || ! proofLHM.get("type").equals("JsonWebSignature2020")) {
      throw new ValidationException("wrong type of proof");
    }

    if(! proofLHM.containsKey("created")) {
      throw new ValidationException("created timestamp not found");
    }

    String created = (String) proofLHM.get("created");

    if (created.isEmpty() || created.isBlank()) {
      throw new ValidationException("created timestamp is empty");
    }

    String regex_iso8601 = "(\\d{4}-\\d{2}-\\d{2})[A-Z]+(\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?).";
    Pattern p = Pattern.compile(regex_iso8601);
    Matcher m = p.matcher(created);
    if(! m.matches()) {
      throw new ValidationException("created timestamp is not in ISO8601 Format");
    }

    if(! proofLHM.containsKey("verificationMethod")) {
      throw new ValidationException("verificationMethod not found");
    }
    String verificationMethod = (String) proofLHM.get("verificationMethod");
    if (verificationMethod.isEmpty() || verificationMethod.isBlank()) {
      throw new ValidationException("verificationMethod is empty");
    }

    if(! proofLHM.containsKey("proofPurpose")) {
      throw new ValidationException("proofPurpose not found");
    }
    String proofPurpose = (String) proofLHM.get("proofPurpose");
    if (proofPurpose.isEmpty() || proofPurpose.isBlank()) {
      throw new ValidationException("proofPurpose is empty");
    }

    if(! proofLHM.containsKey("jws")) {
      throw new ValidationException("jws not found");
    }
    String jws = (String) proofLHM.get("created");
    if (jws.isEmpty() || jws.isBlank()) {
      throw new ValidationException("jws is empty");
    }
  }
}
