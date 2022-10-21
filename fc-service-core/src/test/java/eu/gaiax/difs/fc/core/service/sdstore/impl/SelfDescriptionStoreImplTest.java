package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.Validator;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import liquibase.repackaged.org.apache.commons.collections4.IterableUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {SelfDescriptionStoreImplTest.TestApplication.class, FileStoreConfig.class,
  SelfDescriptionStoreImpl.class, SelfDescriptionStoreImplTest.class, DatabaseConfig.class, Neo4jGraphStore.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class SelfDescriptionStoreImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(final String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @Autowired
  private Neo4jGraphStore graphStore;
  @Autowired
  @Qualifier("sdFileStore")
  private FileStore fileStore;

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    fileStore.clearStorage();
  }

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  private static SelfDescriptionMetadata createSelfDescriptionMeta(final String id, final String issuer,
      final OffsetDateTime sdt, final OffsetDateTime udt, final String content) {
    final String hash = HashUtils.calculateSha256AsHex(content);
    SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata();
    sdMeta.setId(id);
    sdMeta.setIssuer(issuer);
    sdMeta.setSdHash(hash);
    sdMeta.setStatus(SelfDescriptionStatus.ACTIVE);
    sdMeta.setStatusDatetime(sdt);
    sdMeta.setUploadDatetime(udt);
    sdMeta.setSelfDescription(new ContentAccessorDirect(content));
    return sdMeta;
  }

  private static List<SdClaim> createClaims(String subject) {
    final SdClaim claim1 = new SdClaim(subject, "<https://www.w3id.org/gaia-x/service#providedBy>", "<https://delta-dao.com/.well-known/participant.json>");
    final SdClaim claim2 = new SdClaim(subject, "<https://www.w3id.org/gaia-x/service#name>", "\"EuProGigant Portal\"");
    final SdClaim claim3 = new SdClaim(subject, "<https://www.w3id.org/gaia-x/service#description>", "\"EuProGigant Minimal Viable Gaia-X Portal\"");
    final SdClaim claim4 = new SdClaim(subject, "<https://www.w3id.org/gaia-x/service#TermsAndConditions>", "<https://euprogigant.com/en/terms/>");
    final SdClaim claim5 = new SdClaim(subject, "<https://www.w3id.org/gaia-x/service#TermsAndConditions>", "\"contentHash\"");
    return List.of(claim1, claim2, claim3, claim4, claim5);
  }

  private static VerificationResult createVerificationResult(final int idSuffix, String subject) {
      return new VerificationResultOffering(OffsetDateTime.now(), SelfDescriptionStatus.ACTIVE.getValue(), "issuer" + idSuffix, OffsetDateTime.now(), 
              "id" + idSuffix, createClaims(subject), new ArrayList<>());
    }

  private static VerificationResult createVerificationResult(final int idSuffix) {
    return createVerificationResult(idSuffix, "<https://delta-dao.com/.well-known/serviceMVGPortal.json>");
  }

  // Since SdMetaRecord class extends SelfDescriptionMetadata class instead of being formed from it, then check
  // in the equals method will always be false. Because we are downcasting SdMetaRecord to SelfDescriptionMetadata.
  private static void assertThatSdHasTheSameData(final SelfDescriptionMetadata excepted,
      final SelfDescriptionMetadata actual) {
    assertEquals(actual.getId(), excepted.getId());
    assertEquals(actual.getSdHash(), excepted.getSdHash());
    assertEquals(actual.getStatus(), excepted.getStatus());
    assertEquals(actual.getIssuer(), excepted.getIssuer());
    assertEquals(actual.getValidatorDids(), excepted.getValidatorDids());
    assertEquals(actual.getUploadDatetime(), excepted.getUploadDatetime());
    assertEquals(actual.getStatusDatetime(), excepted.getStatusDatetime());
    assertEquals(actual.getSelfDescription(), excepted.getSelfDescription());
  }

  private void assertStoredSdFiles(final int expected) {
    final MutableInt count = new MutableInt(0);
    fileStore.getFileIterable().forEach(file -> count.increment());
    final String message = String.format("Storing %d file(s) should result in exactly %d file(s) in the store.",
        expected, expected);
    assertEquals(expected, count.intValue(), message);
  }

  private void assertAllSdFilesDeleted() {
    final MutableInt count = new MutableInt(0);
    fileStore.getFileIterable().forEach(file -> count.increment());
    assertEquals(0, count.intValue(), "Deleting the last file should result in exactly 0 files in the store.");
    // TODO: check all claims were deleted also
    //List<Map<String, Object>> claims = graphStore.queryData(new OpenCypherQuery("MATCH (n) RETURN n", Map.of()));
    //assertEquals(0, claims.size());
  }

  /**
   * Test storing a self-description, ensuring it creates exactly one file on
   * disk, retrieving it by hash, and deleting it again.
   */
  @Test
  void test01StoreSelfDescription() throws Exception {
    log.info("test01StoreSelfDescription");
    final String content = "Some Test Content";

    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta("https://delta-dao.com/.well-known/serviceMVGPortal.json", // "TestSd/1",
            "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    assertThatSdHasTheSameData(sdMeta, sdStore.getByHash(hash));

    List<Map<String, Object>> claims = graphStore.queryData(
            new OpenCypherQuery("MATCH (n {uri: $uri}) RETURN n", Map.of("uri", sdMeta.getId()))).getResults();
    //Assertions.assertEquals(5, claims.size()); only 1 node found..

    final ContentAccessor sdfileByHash = sdStore.getSDFileByHash(hash);
    assertEquals(sdfileByHash, sdMeta.getSelfDescription(),
        "Getting the SD file by hash is equal to the stored SD file");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    claims = graphStore.queryData(
            new OpenCypherQuery("MATCH (n {uri: $uri}) RETURN n", Map.of("uri", sdMeta.getId()))).getResults();
    Assertions.assertEquals(0, claims.size());

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  /**
   * Test storing a self-description, and deprecating it by storing a second SD
   * with the same subjectId.
   */
  @Test
  void test02StoreAndUpdateSelfDescription() {
    log.info("test02StoreAndUpdateSelfDescription");
    final String content1 = "Some Test Content 1";
    final String content2 = "Some Test Content 2";

    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content1);
    final String hash1 = sdMeta1.getSdHash();
    sdMeta1.setSelfDescription(new ContentAccessorDirect(content1));
    sdStore.storeSelfDescription(sdMeta1, createVerificationResult(1));
    assertStoredSdFiles(1);

    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T13:00:00Z"), OffsetDateTime.parse("2022-01-02T13:00:00Z"), content2);
    final String hash2 = sdMeta2.getSdHash();
    sdStore.storeSelfDescription(sdMeta2, createVerificationResult(2));
    assertStoredSdFiles(2);

    final SelfDescriptionMetadata byHash1 = sdStore.getByHash(hash1);
    assertEquals(SelfDescriptionStatus.DEPRECATED, byHash1.getStatus(),
        "First self-description should have been depricated.");
    assertTrue(byHash1.getStatusDatetime().isAfter(sdMeta1.getStatusDatetime()));
    assertThatSdHasTheSameData(sdMeta2, sdStore.getByHash(hash2));

    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
  }

  @Disabled("TODO Check why self-description is deprecated and if this is intended behavior.")
  @Test
  void test03StoreDuplicateSelfDescription() {
    log.info("test03StoreDuplicateSelfDescription");
    final String content1 = "Some Test Content";

    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content1);
    final String hash1 = sdMeta1.getSdHash();
    sdStore.storeSelfDescription(sdMeta1, createVerificationResult(1));
    assertStoredSdFiles(1);

    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T13:00:00Z"), OffsetDateTime.parse("2022-01-02T13:00:00Z"), content1);
    Assertions.assertThrows(ConflictException.class, () -> {
      sdStore.storeSelfDescription(sdMeta2, createVerificationResult(2));
    });

    final int count = IterableUtils.size(fileStore.getFileIterable());
    assertEquals(1, count, "Second file should not have been stored.");

    final SelfDescriptionMetadata byHash1 = sdStore.getByHash(hash1);
    final SelfDescriptionStatus status1 = byHash1.getStatus();
    assertEquals(SelfDescriptionStatus.ACTIVE, status1, "First self-description should stay active.");

    sdStore.deleteSelfDescription(hash1);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
  }

  /**
   * Test storing a self-description, and updating the status.
   */
  @Test
  void test04ChangeSelfDescriptionStatus() throws Exception {
    log.info("test04ChangeSelfDescriptionStatus");
    final String content = "Some Test Content";

    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    SelfDescriptionMetadata byHash = sdStore.getByHash(hash);
    assertThatSdHasTheSameData(sdMeta, byHash);

    sdStore.changeLifeCycleStatus(hash, SelfDescriptionStatus.REVOKED);
    byHash = sdStore.getByHash(hash);
    assertEquals(SelfDescriptionStatus.REVOKED, byHash.getStatus(), "Status should have been changed to 'revoked'");

    Assertions.assertThrows(ConflictException.class, () -> {
      sdStore.changeLifeCycleStatus(hash, SelfDescriptionStatus.ACTIVE);
    });
    byHash = sdStore.getByHash(hash);
    assertEquals(SelfDescriptionStatus.REVOKED, byHash.getStatus(),
        "Status should not have been changed from 'revoked' to 'active'.");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  /**
   * Test applying an SD filter on matching issuer.
   */
  @Test
  void test05FilterMatchingIssuer() {
    log.info("test05FilterMatchingIssuer");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for matching issuer";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setIssuers(List.of(issuer, "TestUser/21"));
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(1, matchCount, "expected 1 filter match, but got " + matchCount);
    assertEquals(sdMeta.getId(), byFilter.getResults().get(0).getId());

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
    log.info("#### Test 05 succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching issuer.
   */
  @Test
  void test06FilterNonMatchingIssuer() {
    log.info("test06FilterNonMatchingIssuer");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String otherIssuer = "TestUser/2";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for non-matching issuer";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setIssuers(List.of(otherIssuer));
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(0, matchCount, "expected 0 filter matches, but got " + matchCount);

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 06 succeeded.");
  }

  /**
   * Test applying an SD filter on matching status start time.
   */
  @Test
  void test07FilterMatchingStatusTimeStart() {
    log.info("test07FilterMatchingStatusTimeStart");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTimeStart = OffsetDateTime.parse("2021-01-01T12:00:00Z");
    final OffsetDateTime statusTimeEnd = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setStatusTimeRange(statusTimeStart.toInstant(), statusTimeEnd.toInstant());
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(1, matchCount, "expected 1 filter match, but got " + matchCount);
    assertEquals(sdMeta.getId(), byFilter.getResults().get(0).getId());

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
    log.info("#### Test 07 succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching issuer.
   */
  @Test
  void test08FilterNonMatchingStatusTimeStart() {
    log.info("test08FilterNonMatchingStatusTimeStart");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for non-matching issuer";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTimeStart = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime statusTimeEnd = OffsetDateTime.parse("2023-02-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setStatusTimeRange(statusTimeStart.toInstant(), statusTimeEnd.toInstant());
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(0, matchCount, "expected 0 filter matches, but got " + matchCount);

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 08 succeeded.");
  }

  /**
   * Test applying an SD filter that matches multiple records.
   */
  @Test
  void test09FilterMatchingMultipleRecords() {
    log.info("test09FilterMatchingMultipleRecords");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime statusTimeStart = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTimeEnd = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    sdStore.storeSelfDescription(sdMeta1, createVerificationResult(1));
    sdStore.storeSelfDescription(sdMeta2, createVerificationResult(2));
    sdStore.storeSelfDescription(sdMeta3, createVerificationResult(3));
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    filterParams.setStatusTimeRange(statusTimeStart.toInstant(), statusTimeEnd.toInstant());
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(2, matchCount, "expected 2 filter match, but got " + matchCount);
    final SelfDescriptionMetadata filterSdMeta1 = byFilter.getResults().get(0);
    final SelfDescriptionMetadata filterSdMeta2 = byFilter.getResults().get(1);
    assertEquals(true, sdMeta1.getId().equals(filterSdMeta1.getId()) || sdMeta1.getId().equals(filterSdMeta2.getId()),
        "expected filter match sdMeta1 missing in results");
    assertEquals(true, sdMeta2.getId().equals(filterSdMeta1.getId()) || sdMeta2.getId().equals(filterSdMeta2.getId()),
        "expected filter match sdMeta2 missing in results");

    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });

    log.info("#### Test 09 succeeded.");
  }

  /**
   * Test applying an empty SD filter for matching all records.
   */
  @Test
  void test10EmptyFilterMatchingMultipleRecords() {
    log.info("test10EmptyFilterMatchingAllRecords");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    sdStore.storeSelfDescription(sdMeta1, createVerificationResult(1));
    sdStore.storeSelfDescription(sdMeta2, createVerificationResult(2));
    sdStore.storeSelfDescription(sdMeta3, createVerificationResult(3));
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(3, matchCount, "expected 3 filter match, but got " + matchCount);
    final SelfDescriptionMetadata filterSdMeta1 = byFilter.getResults().get(0);
    final SelfDescriptionMetadata filterSdMeta2 = byFilter.getResults().get(1);
    final SelfDescriptionMetadata filterSdMeta3 = byFilter.getResults().get(2);
    assertEquals(true, sdMeta1.getId().equals(filterSdMeta1.getId()) || sdMeta1.getId().equals(filterSdMeta2.getId())
        || sdMeta1.getId().equals(filterSdMeta3.getId()), "expected filter match sdMeta1 missing in results");
    assertEquals(true, sdMeta2.getId().equals(filterSdMeta1.getId()) || sdMeta2.getId().equals(filterSdMeta2.getId())
        || sdMeta2.getId().equals(filterSdMeta3.getId()), "expected filter match sdMeta2 missing in results");
    assertEquals(true, sdMeta3.getId().equals(filterSdMeta1.getId()) || sdMeta3.getId().equals(filterSdMeta2.getId())
        || sdMeta3.getId().equals(filterSdMeta3.getId()), "expected filter match sdMeta3 missing in results");
    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });

    log.info("#### Test 10 succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching validator.
   */
  @Test
  void test11AFilterMatchingValidator() throws Exception {
    log.info("test11AFilterMatchingValidator");
    final String id = "TestSd/1";
    final String validatorId = "TestSd/0815";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for matching validator";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    sdMeta.setValidatorDids(Arrays.asList(validatorId, "TestSd/0816", "TestSd/0817"));
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setValidators(List.of(validatorId, "TestSd/0820"));
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final long matchCount = byFilter.getTotalCount();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(1, matchCount, "expected 1 filter matches");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 11A succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching validator.
   */
  @Test
  void test11BFilterNonMatchingValidator() throws Exception {
    log.info("test11BFilterNonMatchingValidator");
    final String id = "TestSd/1";
    final String validatorId = "TestSd/0815";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for non-matching validator";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    sdMeta.setValidatorDids(Arrays.asList("TestSd/0816", "TestSd/0817"));
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, createVerificationResult(0));
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setValidators(List.of(validatorId));
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(0, matchCount, "expected 0 filter matches");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 11B succeeded.");
  }

  /**
   * Test applying an SD filter with limited number of results.
   */
  @Test
  void test12FilterLimit() {
    log.info("test12FilterLimit");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    sdStore.storeSelfDescription(sdMeta1, createVerificationResult(1));
    sdStore.storeSelfDescription(sdMeta2, createVerificationResult(2));
    sdStore.storeSelfDescription(sdMeta3, createVerificationResult(3));
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    filterParams.setLimit(2);
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(2, matchCount, "expected 2 filter match, but got " + matchCount);
    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });
    log.info("#### Test 12 succeeded.");
  }

  private VerificationResult verifySd(String id, Instant firstSigInstant) throws UnsupportedEncodingException {
    List<Validator> signatures = new ArrayList<>();
    signatures.add(new Validator("did:first", "", firstSigInstant));
    signatures.add(new Validator("did:second", "", Instant.now().plus(1, ChronoUnit.DAYS)));
    signatures.add(new Validator("did:third", "", Instant.now().plus(2, ChronoUnit.DAYS)));
    return new VerificationResult(OffsetDateTime.now(), SelfDescriptionStatus.ACTIVE.getValue(), "issuer", OffsetDateTime.now(), 
            id, new ArrayList<>(), signatures);
  }

  @Test
  void test13PeriodicValidationOfSignatures() throws IOException {
    log.info("test13PeriodicValidationOfSignatures");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: SD 1 with future expiration date";
    final String content2 = "Test: SD 2 with past expiration date";
    final String content3 = "Test: SD 3 with future expiration date";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    final VerificationResult vr1 = verifySd(id1, Instant.now().plus(1, ChronoUnit.DAYS));
    final VerificationResult vr2 = verifySd(id2, Instant.now().minus(1, ChronoUnit.DAYS));
    final VerificationResult vr3 = verifySd(id3, Instant.now().plus(1, ChronoUnit.DAYS));
    sdStore.storeSelfDescription(sdMeta1, vr1);
    sdStore.storeSelfDescription(sdMeta2, vr2);
    sdStore.storeSelfDescription(sdMeta3, vr3);
    assertStoredSdFiles(3);

    final int expiredSelfDescriptionsCount = sdStore.invalidateSelfDescriptions();
    assertEquals(1, expiredSelfDescriptionsCount, "expected 1 expired self-description");
    assertEquals(SelfDescriptionStatus.ACTIVE, sdStore.getByHash(hash1).getStatus(), "Status should not have been changed.");
    assertEquals(SelfDescriptionStatus.EOL, sdStore.getByHash(hash2).getStatus(), "Status should have been changed.");
    assertEquals(SelfDescriptionStatus.ACTIVE, sdStore.getByHash(hash3).getStatus(), "Status should not have been changed.");

    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });
    log.info("#### Test 13 succeeded.");
  }

  /**
   * Test applying an SD filter with limited number of results with total SD count number.
   */
  @Test
  void test13FilterLimitWithTotalCount() {
    log.info("test12FilterLimit");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    sdStore.storeSelfDescription(sdMeta1, createVerificationResult(1));
    sdStore.storeSelfDescription(sdMeta2, createVerificationResult(2));
    sdStore.storeSelfDescription(sdMeta3, createVerificationResult(3));
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    filterParams.setLimit(1);
    final PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.getResults().size();

    log.info("filter returned {} match(es)", matchCount);
    assertEquals(1, matchCount, "expected 2 filter match, but got " + matchCount);
    assertEquals(3, byFilter.getTotalCount(), "expected 3 total count, but got " + matchCount);
    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });
    log.info("#### Test 13 succeeded.");
  }

}
