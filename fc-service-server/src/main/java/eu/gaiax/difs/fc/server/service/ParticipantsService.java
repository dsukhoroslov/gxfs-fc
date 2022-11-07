package eu.gaiax.difs.fc.server.service;

import static eu.gaiax.difs.fc.server.util.SessionUtils.checkParticipantAccess;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.Participants;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import eu.gaiax.difs.fc.core.dao.ParticipantDao;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.pojo.ParticipantMetaData;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.generated.controller.ParticipantsApiDelegate;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementation of the {@link ParticipantsApiDelegate} interface.
 */
@Slf4j
@Service
public class ParticipantsService implements ParticipantsApiDelegate {
  @Autowired
  private ParticipantDao partDao;
  @Autowired
  @Qualifier("sdFileStore")
  private FileStore fileStore;
  @Autowired
  private SelfDescriptionStore selfDescriptionStore;
  @Autowired
  private VerificationService verificationService;

  /**
   * POST /participants : Register a new participant in the catalogue.
   *
   * @param body Participant Self-Description (required)
   * @return Created Participant (status code 201)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  @Transactional
  public ResponseEntity<Participant> addParticipant(String body) {
    log.debug("addParticipant.enter; got SD of length: {}", body.length()); // it can be JWT?
    Pair<VerificationResultParticipant, SelfDescriptionMetadata> pairResult = validateSelfDescription(body);
    VerificationResultParticipant verificationResult = pairResult.getLeft();
    SelfDescriptionMetadata selfDescriptionMetadata = pairResult.getRight();

    selfDescriptionStore.storeSelfDescription(selfDescriptionMetadata, verificationResult);

    ParticipantMetaData participantMetaData = toParticipantMetaData(verificationResult, selfDescriptionMetadata);
    registerRollBackForFileStoreManuallyIfTransactionFail(participantMetaData);

    participantMetaData = partDao.create(participantMetaData);
    return ResponseEntity.created(URI.create("/participants/" + participantMetaData.getId())).body(participantMetaData);
  }

  /**
   * DELETE /participants/{participantId} : Delete a participant in the catalogue.
   *
   * @param participantId The participant to delete. (required)
   * @return Deleted Participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  @Transactional
  public ResponseEntity<Participant> deleteParticipant(String participantId) {
    String partId = URLDecoder.decode(participantId, Charset.defaultCharset());  
    log.debug("deleteParticipant.enter; got participant: {}, id: {}", participantId, partId);
    checkParticipantAccess(partId);
    ParticipantMetaData part = partDao.select(partId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + partId));
    String selfDescription = selfDescriptionStore.getByHash(part.getSdHash()).getSelfDescription().getContentAsString();
    selfDescriptionStore.deleteSelfDescription(part.getSdHash());
    part = partDao.delete(part.getId()).get();
    part.setSelfDescription(selfDescription);
    log.debug("deleteParticipant.exit; returning: {}", part);
    return ResponseEntity.ok(part);
  }

  /**
   * GET /participants/{participantId} : Get the registered participant.
   *
   * @param participantId The participantId to get. (required)
   * @return The requested participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Participant> getParticipant(String participantId) {
    String partId = URLDecoder.decode(participantId, Charset.defaultCharset());  
    log.debug("getParticipant.enter; got participant: {}, id: {}", participantId, partId);
    checkParticipantAccess(partId);
    ParticipantMetaData part = partDao.select(partId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + partId));
    SelfDescriptionMetadata selfDescriptionMetadata = selfDescriptionStore.getByHash(part.getSdHash());
    part.setSelfDescription(selfDescriptionMetadata.getSelfDescription().getContentAsString());
    log.debug("getParticipant.exit; returning: {}", part);
    return ResponseEntity.ok(part);
  }

  /**
   * GET /participants/{participantId}/users : Get all users of the registered participant.
   *
   * @param participantId The participant Id (required)
   * @return Users of the participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfiles> getParticipantUsers(String participantId, Integer offset, Integer limit) {
    String partId = URLDecoder.decode(participantId, Charset.defaultCharset());  
    log.debug("getParticipantUsers.enter; got participantId: {}, id: {}", participantId, partId);
    checkParticipantAccess(partId);
    PaginatedResults<UserProfile> profiles = partDao.selectUsers(partId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + partId));
    log.debug("getParticipantUsers.exit; returning: {}", profiles.getTotalCount());
    return ResponseEntity.ok(new UserProfiles((int) profiles.getTotalCount(), profiles.getResults()));
  }

  /**
   * GET /participants : Get the registered participants.
   *
   * @param offset The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit  The number of items to return. (optional, default to 100)
   * @return List of registered participants (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Participants> getParticipants(Integer offset, Integer limit) { //String orderBy, Boolean asc) {
    // sorting is not supported yet by keycloak admin API
    log.debug("getParticipants.enter; got offset: {}, limit: {}", offset, limit);
    PaginatedResults<ParticipantMetaData> results = partDao.search(offset, limit);
    //Adding actual SD from sd-store for each sd-hash present in keycloak
    SdFilter filter = new SdFilter();
    filter.setLimit(results.getResults().size());
    filter.setOffset(0);
    filter.setHashes(results.getResults().stream().map(ParticipantMetaData::getSdHash).collect(Collectors.toList()));
    Map<String, ContentAccessor> sdsMap = selfDescriptionStore.getByFilter(filter).getResults().stream()
        .collect(Collectors.toMap(SelfDescriptionMetadata::getSdHash, SelfDescriptionMetadata::getSelfDescription));
    results.getResults().forEach(part -> part.setSelfDescription(sdsMap.get(part.getSdHash()).getContentAsString()));
    log.debug("getParticipants.exit; returning results: {}", results);
    int total = (int) results.getTotalCount();
    List parts = results.getResults();
    return ResponseEntity.ok(new Participants(total, parts));
  }

  /**
   * PUT /participants/{participantId} : Update a participant in the catalogue.
   *
   * @param participantId The participant to update. (required)
   * @param body          Participant Self-Description (required)
   * @return Updated Participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  @Transactional
  public ResponseEntity<Participant> updateParticipant(String participantId, String body) {
    String partId = URLDecoder.decode(participantId, Charset.defaultCharset());  
    log.debug("updateParticipant.enter; got participant: {}, id: {}", participantId, partId);
    checkParticipantAccess(partId);

    ParticipantMetaData participantExisted = partDao.select(partId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + partId));

    Pair<VerificationResultParticipant, SelfDescriptionMetadata> pairResult = validateSelfDescription(body);
    VerificationResultParticipant verificationResult = pairResult.getLeft();
    SelfDescriptionMetadata selfDescriptionMetadata = pairResult.getRight();

    ParticipantMetaData participantUpdated = toParticipantMetaData(verificationResult, selfDescriptionMetadata);
    if (!participantUpdated.getId().equals(participantExisted.getId())) {
      throw new ClientException("Participant ID cannot be changed");
    }

    selfDescriptionStore.storeSelfDescription(selfDescriptionMetadata, verificationResult);
    registerRollBackForFileStoreManuallyIfTransactionFail(participantUpdated);

    ParticipantMetaData participantMetaData = partDao.update(partId, participantUpdated)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + partId));
    log.debug("updateParticipant.exit; returning: {}", participantMetaData);
    participantMetaData.setSelfDescription(selfDescriptionStore.getByHash(participantMetaData.getSdHash()).getSelfDescription().getContentAsString());
    return ResponseEntity.ok(participantMetaData);
  }

  /**
   * Utility method to return {@link ParticipantMetaData}.
   *
   * @param verificationResult      Result of validation
   * @param selfDescriptionMetadata Metadata of self-description
   * @return ParticipantMetaData
   */
  private ParticipantMetaData toParticipantMetaData(VerificationResultParticipant verificationResult,
                                                    SelfDescriptionMetadata selfDescriptionMetadata) {
    return new ParticipantMetaData(verificationResult.getId(), verificationResult.getParticipantName(),
        verificationResult.getParticipantPublicKey(), selfDescriptionMetadata.getSelfDescription().getContentAsString(),
        selfDescriptionMetadata.getSdHash());
  }

  /**
   * Validate self-Description.
   *
   * @param body self description
   * @return DTO object containing result and metadata of self-description
   */
  private Pair<VerificationResultParticipant, SelfDescriptionMetadata> validateSelfDescription(String body) {
    ContentAccessorDirect contentAccessorDirect = new ContentAccessorDirect(body);
    VerificationResultParticipant verificationResultParticipant =
        verificationService.verifyParticipantSelfDescription(contentAccessorDirect);
    log.debug("validateSelfDescription; verification result is: {}", verificationResultParticipant);

    SelfDescriptionMetadata selfDescriptionMetadata = new SelfDescriptionMetadata(contentAccessorDirect, verificationResultParticipant);
    log.debug("validateSelfDescription; SD metadata is: {}", selfDescriptionMetadata);

    return Pair.of(verificationResultParticipant, selfDescriptionMetadata);
  }

  /**
   * Manually registering rollback for the file system when transactions was rolled-back as spring does not have
   * rollback for file systems storage.
   *
   * @param part participant metadata to be rolled back.
   */
  private void registerRollBackForFileStoreManuallyIfTransactionFail(ParticipantMetaData part) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
          try {
            fileStore.deleteFile(part.getSdHash());
            log.debug("registerRollBackForFileStoreManuallyIfTransactionFail; Rolling back file with hash: {}",
                    part.getSdHash());
            TransactionSynchronizationManager.clearSynchronization();
          } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
          }
        }
      }
    });
  }

}
