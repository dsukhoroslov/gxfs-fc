package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class ValidationResultOffering {
    public ValidationResultOffering (
            String id,
            String issuer,
            String verificationTimestamp,
            String lifecycleStatus,
            Instant issuedDate,
            List<Signature> signatures,
            List<Claim> claims
    ) {
        this.id = id;
        this.issuer = issuer;
        this.verificationTimestamp = verificationTimestamp;
        this.lifecycleStatus = lifecycleStatus;
        this.issuedDate = issuedDate;
        this.signatures = signatures;
        this.claims = claims;
    }
    String id; // credentialSubject (id) of this SD
    String issuer; // credentialSubject (id) of the participant owning this SD
    String verificationTimestamp;
    String lifecycleStatus; // is not known when verifying external SDs, or do we search by ID if we have this locally?
    Instant issuedDate;
    List<Signature> signatures; //Signature details unknown
    List<Claim> claims; // claims of the SD, to be inserted into the Graph-DB

    public String getId() {
        return id;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public Instant getIssuedDate() {
        return issuedDate;
    }

    public List<Signature> getSignatures() {
        return signatures;
    }

    public List<Claim> getClaims() {
        return claims;
    }
}