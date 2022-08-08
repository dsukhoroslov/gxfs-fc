package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VerificationResult extends eu.gaiax.difs.fc.api.generated.model.VerificationResult
{
    String id; // credentialSubject (id) of this SD
    String verificationTimestamp;
    String lifecycleStatus; // is not known when verifying external SDs, or do we search by ID if we have this locally?
    Instant issuedDate;
    List<Signature> signatures; //Signature details unknown
    List<SdClaim> claims; // claims of the SD, to be inserted into the Graph-DB

    VerificationResult(String id, String verificationTimestamp, String lifecycleStatus, Instant issuedDate, List<Signature> signatures, List<SdClaim> claims) {
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

    public String getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getIssuedDate() {
        return issuedDate.toString();
    }

    public List<Object> getSignatures() {
        return new ArrayList<>(signatures);
    }

    public List<SdClaim> getClaims() {
        return claims;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setVerificationTimestamp(String verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    @Override
    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public void setIssuedDate(Instant issuedDate) {
        this.issuedDate = issuedDate;
    }

    @Override
    public void setSignatures(List<Object> signatures) {
        //TODO: Fix this when t-systems adopted the new interface
        List list = (List) signatures;
        this.signatures = (List<Signature>) list;
    }

    public void setClaims(List<SdClaim> claims) {
        this.claims = claims;
    }
}
