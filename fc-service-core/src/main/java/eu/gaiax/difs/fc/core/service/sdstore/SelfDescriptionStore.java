package eu.gaiax.difs.fc.core.service.sdstore;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import java.util.List;

/**
 *
 * @author hylke
 */
public interface SelfDescriptionStore {

    /**
     * Get all self descriptions, starting from the given offset, up to limit
     * number of items, consistently ordered.
     *
     * @param offset How many items to skip.
     * @param limit The maximum number of items to return.
     * @return
     */
    public List<SelfDescriptionMetadata> getAllSelfDescriptions(int offset, int limit);

    /**
     * Fetch a SelfDescription and its meta data by hash.
     * @param hash
     * @return
     */
    public SelfDescriptionMetadata getByHash(String hash);

    /**
     * Fetch all SelfDescriptions that match the filter parameters.
     * @param filterParams
     * @return
     */
    public List<SelfDescriptionMetadata> getByFilter(SdFilter filterParams);

    /**
     * Store the given SelfDescription.
     * @param selfDescription
     * @param sdVerificationResults
     */
    public void storeSelfDescription(SelfDescriptionMetadata selfDescription, VerificationResult sdVerificationResults);

    /**
     * Change the life cycle status of the self description with the given hash.
     * @param hash The hash of the SD to work on.
     * @param targetStatus The new status.
     */
    public void changeLifeCycleStatus(String hash, SelfDescription.StatusEnum targetStatus);

    /**
     * Remove the Self-Description with the given hash from the store.
     * @param hash The hash of the SD to work on.
     */
    public void deleteSelfDescription(String hash);
}
