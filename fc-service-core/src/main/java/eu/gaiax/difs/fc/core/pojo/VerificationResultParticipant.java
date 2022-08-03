package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class VerificationResultParticipant extends VerificationResult{

    public VerificationResultParticipant(
            String participantName,
            String id,
            String participantPublicKey,
            String verificationTimestamp,
            String lifecycleStatus,
            Instant issuedDate,
            List<Signature> signatures,
            List<Claim> claims
    ) {
        super(id, verificationTimestamp, lifecycleStatus, issuedDate, signatures, claims);
        this.participantName = participantName;
        this.participantPublicKey = participantPublicKey;
    }

    String participantName; // Name of the Participant
    String participantPublicKey; // The public key of the participant

    public String getParticipantName() {
        return participantName;
    }

    public String getParticipantPublicKey() {
        return participantPublicKey;
    }
}

