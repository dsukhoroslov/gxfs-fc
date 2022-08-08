package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class VerificationResultParticipant extends VerificationResult{

    String participantName; // Name of the Participant
    String participantPublicKey; // The public key of the participant

    /**
     * Constructor for the VerificationResultParticipant
     * @param participantName Name of participant
     * @param id id of SD
     * @param participantPublicKey public key of participant
     * @param verificationTimestamp time stamp of verification
     * @param lifecycleStatus status according to GAIA-X lifecycle
     * @param issuedDate issuing date of the SD
     * @param signatures List of signatures in the SD
     * @param claims List of claims in the SD
     */
    public VerificationResultParticipant(
            String participantName,
            String id,
            String participantPublicKey,
            String verificationTimestamp,
            String lifecycleStatus,
            Instant issuedDate,
            List<Signature> signatures,
            List<SdClaim> claims
    ) {
        super(id, verificationTimestamp, lifecycleStatus, issuedDate, signatures, claims);
        this.participantName = participantName;
        this.participantPublicKey = participantPublicKey;
    }

    public String getParticipantName() {
        return participantName;
    }

    public String getParticipantPublicKey() {
        return participantPublicKey;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public void setParticipantPublicKey (String participantPublicKey) {
        this.participantPublicKey = participantPublicKey;
    }

}

