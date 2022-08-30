package eu.gaiax.difs.fc.core.service.integration;

import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationServiceImpl;
import eu.gaiax.difs.fc.core.util.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class IntegrationTests {

    @Test
    public void testComponentInteraction () throws UnsupportedEncodingException {
        //get SD
        ContentAccessor accessor = FileUtils.getAccessorByPath("Integration/serviceAnalytics.json");

        //verify it
        VerificationService verificationService = new VerificationServiceImpl();
        VerificationResult verificationResult = verificationService.verifyOfferingSelfDescription(accessor);

        //process result
        SelfDescriptionMetadata metadata = new SelfDescriptionMetadata(
                accessor,
                null, //TODO how to determine it?
                verificationResult.getIssuer(),
                null);// not yet implement, but not yet required

        //pass it to SDStorage
        SelfDescriptionStore sdStore = new SelfDescriptionStoreImpl();
        sdStore.storeSelfDescription(metadata, verificationResult);

        //access GraphDB to check if SD was stored
        //TODO
    }
}
