package eu.gaiax.difs.fc.core.pojo;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import java.time.Instant;
import java.util.List;

/**
 *
 * @author hylke
 */
public class SelfDescriptionMetadata {

    private String sdHash;
    /**
     * credentialSubject (id) of this SD.
     */
    private String id;
    /**
     * Status of the SelfDescription in the catalogue.
     */
    private SelfDescription.StatusEnum status;
    /**
     * credentialSubject (id) of the participant owning this SD.
     */
    private String issuer;
    /**
     * The time stamp (ISO8601) when the SD was uploaded.
     */
    private Instant uploadTime;
    /**
     * The last time stamp (ISO8601) the status changed (for this Catalogue).
     */
    private Instant statusTime;
    /**
     * The credentialSubjects (ids) of the validators.
     */
    private List<String> validators;
    /**
     * A reference to the self description content.
     */
    private SelfDescriptionContent selfDescription;

    public SelfDescriptionMetadata() {
    }

    public String getSdHash() {
        return sdHash;
    }

    public void setSdHash(String sdHash) {
        this.sdHash = sdHash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SelfDescription.StatusEnum getStatus() {
        return status;
    }

    public void setStatus(SelfDescription.StatusEnum status) {
        this.status = status;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Instant getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Instant uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Instant getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(Instant statusTime) {
        this.statusTime = statusTime;
    }

    public List<String> getValidators() {
        return validators;
    }

    public void setValidators(List<String> validators) {
        this.validators = validators;
    }

    public SelfDescriptionContent getSelfDescription() {
        return selfDescription;
    }

    public void setSelfDescription(SelfDescriptionContent selfDescription) {
        this.selfDescription = selfDescription;
    }

}
