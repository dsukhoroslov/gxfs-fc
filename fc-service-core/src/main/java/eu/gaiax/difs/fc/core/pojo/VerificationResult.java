package eu.gaiax.difs.fc.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VerificationResult extends eu.gaiax.difs.fc.api.generated.model.VerificationResult {

  /**
   * credentialSubject (id) of this SD.
   */
  @JsonIgnore
  private String id;
  /**
   * claims of the SD, to be inserted into the Graph-DB.
   */
  @JsonIgnore
  private List<SdClaim> claims;
  /**
   * validators, that signed parts of the SD.
   */
  @JsonIgnore
  private List<Validator> validators;

  public VerificationResult(String id, List<SdClaim> claims, List<Validator> validators, OffsetDateTime verificationTimestamp, String lifecycleStatus, String issuer, LocalDate issuedDate) {
    super(verificationTimestamp, lifecycleStatus, issuer, issuedDate, null);
    this.id = id;
    this.claims = claims;
    this.validators = validators;
    //TODO: Check what parts of the validators should be added to the response
    List<Object> sigObs = new ArrayList<>(validators);
    List<List<Object>> sigs2 = Arrays.asList(sigObs);
    super.setSignatures(sigs2);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<SdClaim> getClaims() {
    return claims;
  }

  public void setClaims(List<SdClaim> claims) {
    this.claims = claims;
  }

  public List<Validator> getValidators() {
    return validators;
  }

  public void setValidators(List<Validator> validators) {
    this.validators = validators;
  }
}
