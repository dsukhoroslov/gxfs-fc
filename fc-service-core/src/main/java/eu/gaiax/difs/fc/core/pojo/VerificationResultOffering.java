package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class VerificationResultOffering extends VerificationResult {
    String issuer; // credentialSubject (id) of the participant owning this SD

    /**
     * Constructor for the VerificationResultParticipant
     * @param id id of SD
     * @param verificationTimestamp time stamp of verification
     * @param lifecycleStatus status according to GAIA-X lifecycle
     * @param issuedDate issuing date of the SD
     * @param signatures List of signatures in the SD
     * @param claims List of claims in the SD
     * @param issuer Issuer of the offering
     */
    public VerificationResultOffering(
            String id,
            String issuer,
            String verificationTimestamp,
            String lifecycleStatus,
            Instant issuedDate,
            List<Signature> signatures,
            List<SdClaim> claims
    ) {
        super(id, verificationTimestamp, lifecycleStatus, issuedDate, signatures, claims);
        this.issuer = issuer;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}