package eu.gaiax.difs.fc.core.service.validation.impl;

import com.github.jsonldjava.utils.JsonUtils;
import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.core.dto.ShaclModel;
import eu.gaiax.difs.fc.core.exception.ValidationException;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.Signature;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.validation.ValidationService;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;
import org.springframework.stereotype.Service;


// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link ValidationService} interface.
 */
@Service
public class ValidationServiceImpl implements ValidationService {
  private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
  private static final Path BASE_PATH = Paths.get(".").toAbsolutePath().normalize();
  private static final Marker WTF_MARKER = MarkerFactory.getMarker("WTF");

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public Participant validateParticipantSelfDescription(String json) throws ValidationException {
    return null;
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(String json) throws ValidationException {
    boolean verifyOffering = true;
    String id = "";
    String issuer = "";
    String verificationTimestamp = "";
    String lifecycleStatus = "";
    String participantName = "";
    String participantPublicKey = "";
    Instant issuedDate = Instant.now();
    List<Signature> signatures = null;
    List<SdClaim> claims = null;

    //TODO: Verify Syntax FIT-WI
    Map<String, Object> parsedSD = parseSD(json);

    //TODO: Verify Cryptographic FIT-WI

    //TODO: Verify Schema FIT-DSAI

    //TODO: Extract Claims FIT-DSAI

    //TODO: Check if API-User is allowed to submit the self-description FIT

    //Decide what to return
    if (verifyOffering) {
      return new VerificationResultOffering(
              id,
              issuer,
              verificationTimestamp,
              lifecycleStatus,
              issuedDate,
              signatures,
              claims
      );
    } else {
      return new VerificationResultParticipant(
              participantName,
              id,
              participantPublicKey,
              verificationTimestamp,
              lifecycleStatus,
              issuedDate,
              signatures,
              claims
      );
    }
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param json The json which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public SelfDescription validateSelfDescription(String json) throws ValidationException {
    SelfDescription sdMetadata = new SelfDescription();
    sdMetadata.setId("string");
    sdMetadata.setSdHash("string");
    sdMetadata.setIssuer("http://example.org/test-provider");
    sdMetadata.setStatus(SelfDescription.StatusEnum.ACTIVE);
    List<String> validators = new ArrayList<>();
    validators.add("string");
    sdMetadata.setValidators(validators);
    sdMetadata.setStatusTime("2022-05-11T15:30:00Z");
    sdMetadata.setUploadTime("2022-03-01T13:00:00Z");
    return sdMetadata;
  }

  /*package private functions*/

  Map<String, Object> parseSD (String json) throws ValidationException {
    Object parsed;
    try {
      parsed = JsonUtils.fromString(json);
    } catch (IOException e) {
      throw new ValidationException(e.getMessage());
    }

    return (Map<String, Object>) parsed;
  }


  /**
   * Validate a datagraph against shaclShape from pre-defined files stored in the file system
   *
   * @param dataGraphPath   string indictates the data graph file path
   * @param shaclShapesPath string indictates the shacl shapes file path
   * @return                Serialization for The JSON report Result
   */
  public String validate(String dataGraphPath, String shaclShapesPath) {
    String reportResult = "";
    ShaclModel shaclModel = null;
    OutputStream reportOutputStream = null;
    try {
      String data = BASE_PATH .toFile().getAbsolutePath() + dataGraphPath;
      String shape = BASE_PATH .toFile().getAbsolutePath() + shaclShapesPath;
      Model dataModel = JenaUtil.createDefaultModel();
      dataModel.read(data);
      Model shapeModel = JenaUtil.createDefaultModel();
      shapeModel.read(shape);

      Resource reportResource = ValidationUtil.validateModel(dataModel, shapeModel, true);
      boolean conforms = reportResource.getProperty(SH.conforms).getBoolean();
      logger.trace("Conforms = " + conforms);

      if (!conforms) {
        String report = BASE_PATH.toFile().getAbsolutePath() + "/src/test/resources/Validation-Tests/report.ttl";
        File reportFile = new File(report);
        reportFile.createNewFile();
        reportOutputStream = new FileOutputStream(reportFile);

        RDFDataMgr.write(reportOutputStream, reportResource.getModel(), RDFFormat.JSONLD);
        try {
          Scanner scanner = new Scanner(reportFile);
          while (scanner.hasNextLine()) {
            reportResult += scanner.nextLine();
          }
          scanner.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }

      }

    } catch (Throwable t) {
      logger.error(WTF_MARKER, t.getMessage(), t);
    }

    return reportResult;
  }


}
