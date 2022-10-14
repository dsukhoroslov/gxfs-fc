package eu.gaiax.difs.fc.core.pojo;

import static eu.gaiax.difs.fc.core.util.HashUtils.calculateSha256AsHex;
import static java.time.OffsetDateTime.now;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Class for handling the metadata of a Self-Description, and optionally a
 * reference to a content accessor.
 */
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class SelfDescriptionMetadata extends SelfDescription {

  /**
   * A reference to the self description content.
   */
  @lombok.Getter
  @lombok.Setter
  @JsonIgnore
  private ContentAccessor selfDescription;

  public SelfDescriptionMetadata(String id, String issuer, List<Validator> validators, ContentAccessor contentAccessor) {
    super(calculateSha256AsHex(contentAccessor.getContentAsString()), id, SelfDescriptionStatus.ACTIVE, issuer, 
            validators.stream().map(Validator::getDidURI).collect(Collectors.toList()), now(), now());
    this.selfDescription = contentAccessor;
  }

  public SelfDescriptionMetadata(ContentAccessorDirect contentAccessorDirect, VerificationResultParticipant verificationResult) {
    super(calculateSha256AsHex(contentAccessorDirect.getContentAsString()), verificationResult.getId(), SelfDescriptionStatus.ACTIVE,
            verificationResult.getIssuer(), Collections.emptyList(), verificationResult.getVerificationTimestamp(),
            verificationResult.getVerificationTimestamp());
    this.selfDescription = contentAccessorDirect;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(this.getSdHash());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (getClass() != obj.getClass()) {
        return false;
    }
    SelfDescriptionMetadata other = (SelfDescriptionMetadata) obj;
    return Objects.equals(this.getSdHash(), other.getSdHash());
  }
  
}
