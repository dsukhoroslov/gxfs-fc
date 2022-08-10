package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VerificationResult extends eu.gaiax.difs.fc.api.generated.model.VerificationResult
{
    private String id; // credentialSubject (id) of this SD
    private String verificationTimestamp;
    private String lifecycleStatus; // is not known when verifying external SDs, or do we search by ID if we have this locally?
    private Instant issuedDate;
    private List<Signature> signatures; //Signature details unknown
    private List<Claim> claims; // claims of the SD, to be inserted into the Graph-DB

    public VerificationResult(String id, String verificationTimestamp, String lifecycleStatus, Instant issuedDate, List<Signature> signatures, List<Claim> claims) {
        super(verificationTimestamp, lifecycleStatus, null, issuedDate.toString(), new ArrayList<>(signatures));

        this.id = id;
        this.verificationTimestamp = verificationTimestamp;
        this.lifecycleStatus = lifecycleStatus;
        this.issuedDate = issuedDate;
        this.signatures = signatures;
        this.claims = claims;
    }

    public VerificationResult(String id, String verificationTimestamp, String lifecycleStatus, Instant issuedDate, List<Signature> signatures, List<Claim> claims, Object issuer) {
        super(verificationTimestamp, lifecycleStatus, issuer, issuedDate.toString(), new ArrayList<>(signatures));

        this.id = id;
        this.verificationTimestamp = verificationTimestamp;
        this.lifecycleStatus = lifecycleStatus;
        this.issuedDate = issuedDate;
        this.signatures = signatures;
        this.claims = claims;
    }

    public String getId() {
        return id;
    }

    public List<Object> getSignatures() {
        return new ArrayList<>(signatures);
    }

    public List<Claim> getClaims() {
        return claims;
    }
}
