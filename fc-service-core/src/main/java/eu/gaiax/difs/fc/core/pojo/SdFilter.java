package eu.gaiax.difs.fc.core.pojo;

import java.util.Optional;

/**
 *
 * @author hylke
 */
public class SdFilter {

    /**
     * Filter for the time range when the Self-Description was uploaded to the
     * catalogue. The time range has to be specified as start time and end time
     * as ISO8601 timestamp separated by a `/`. example:
     * '2022-03-01T13:00:00Z/2022-05-11T15:30:00Z
     */
    private Optional<String> uploadTimerange;
    /**
     * Filter for the time range when the status of the Self-Description was
     * last changed in the catalogue. The time range has to be specified as
     * start time and end time as ISO8601 timestamp separated by a `/`. example:
     * '2022-03-01T13:00:00Z/2022-05-11T15:30:00Z
     */
    private Optional<String> statusTimerange;
    /**
     * Filter for the issuer of the Self-Description. This is the unique ID
     * (credentialSubject) of the Participant that has prepared the
     * Self-Description.
     */
    private Optional<String> issuer;
    /**
     * Filter for a validator of the Self-Description. This is the unique ID
     * (credentialSubject) of the Participant that validated (part of) the
     * Self-Description.
     */
    private Optional<String> validator;
    /**
     * Filter for the status of the Self-Description. Values: active, eol,
     * deprecated, revoked
     */
    private Optional<String> status;
    /**
     * Filter for a id/credentialSubject of the Self-Description.
     */
    private Optional<String> id;
    /**
     * Filter for a hash of the Self-Description.
     */
    private Optional<String> hash;
    /**
     * Filter for the issuer of the Self-Description. This is the unique ID
     * (credentialSubject) of the Participant that has prepared the
     * Self-Description.
     */
    private Optional<Integer> offset;
    /**
     * Filter for the issuer of the Self-Description. This is the unique ID
     * (credentialSubject) of the Participant that has prepared the
     * Self-Description.
     */
    private Optional<Integer> limit;

    /**
     * Filter for the issuer of the Self-Description. This is the unique ID
     * (credentialSubject) of the Participant that has prepared the
     * Self-Description.
     *
     * @return the limit
     */
    public Optional<Integer> getLimit() {
        return limit;
    }

    /**
     * Filter for the issuer of the Self-Description. This is the unique ID
     * (credentialSubject) of the Participant that has prepared the
     * Self-Description.
     *
     * @param limit the limit to set
     */
    public void setLimit(Optional<Integer> limit) {
        this.limit = limit;
    }

}
