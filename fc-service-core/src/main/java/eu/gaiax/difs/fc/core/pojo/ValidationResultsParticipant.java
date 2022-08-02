package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class ValidationResultsParticipant {

    public ValidationResultsParticipant(
            String participantName,
            String participantId,
            String participantPublicKey,
            String verificationTimestamp,
            String lifecycleStatus,
            Instant issuedDate,
            List<Signature> signatures,
            List<Claim> claims
    ) {
        this.participantName = participantName;
        this.participantId = participantId;
        this.participantPublicKey = participantPublicKey;
        this.verificationTimestamp = verificationTimestamp;
        this.lifecycleStatus = lifecycleStatus;
        this.issuedDate = issuedDate;
        this.signatures = signatures;
        this.claims = claims;
    }

    String participantName; // Name of the Participant
    String participantId; // credentialSubject (id) of the participant owning this SD
    String participantPublicKey; // The public key of the participant
    String verificationTimestamp;
    String lifecycleStatus; // is not known when verifying external SDs, or do we search by ID if we have this locally?
    Instant issuedDate;
    List<Signature> signatures; //Signature details unknown

    public String getParticipantName() {
        return participantName;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getParticipantPublicKey() {
        return participantPublicKey;
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

    List<Claim> claims; // claims of the SD, to be inserted into the Graph-DB
}

