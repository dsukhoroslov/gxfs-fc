package eu.gaiax.difs.fc.core.pojo;

import static eu.gaiax.difs.fc.core.util.HashUtils.calculateSha256AsHex;
import static java.time.OffsetDateTime.now;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for handling the metadata of a Self-Description, and optionally a
 * reference to a content accessor.
 */
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)
public class SelfDescriptionMetadata extends SelfDescription {

  /**
   * A reference to the self description content.
   */
  @lombok.Getter
  @lombok.Setter
  @JsonIgnore
  private ContentAccessor selfDescription;

  public SelfDescriptionMetadata(ContentAccessor contentAccessor, String id, String issuer, List<String> validators) {
    super(calculateSha256AsHex(contentAccessor.getContentAsString()), id, SelfDescriptionStatus.ACTIVE, issuer, validators, now(), now());
    this.selfDescription = contentAccessor;
  }

  public SelfDescriptionMetadata(ContentAccessor contentAccessor, VerificationResultParticipant verificationResult) {
    super(calculateSha256AsHex(contentAccessor.getContentAsString()), verificationResult.getId(), SelfDescriptionStatus.ACTIVE,
         verificationResult.getIssuer(),Collections.<String>emptyList(),verificationResult.getVerificationTimestamp(),
        verificationResult.getVerificationTimestamp());

    this.selfDescription = contentAccessor;
  }
}
