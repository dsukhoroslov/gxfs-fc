package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class VerificationResultOffering extends VerificationResult {
    public VerificationResultOffering(
            String id,
            String issuer,
            String verificationTimestamp,
            String lifecycleStatus,
            Instant issuedDate,
            List<Signature> signatures,
            List<Claim> claims
    ) {
        super(id, verificationTimestamp, lifecycleStatus, issuedDate, signatures, claims, issuer);
    }
}