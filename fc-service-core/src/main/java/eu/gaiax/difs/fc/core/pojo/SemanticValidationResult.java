package eu.gaiax.difs.fc.core.pojo;

/**
 * POJO Class for holding Semantic Validation Results.
 */
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.Setter
public class SemanticValidationResult {
    private final boolean conforming;
    private final String validationReport;

    public SemanticValidationResult(boolean conforming,String validationReport) {
        this.conforming = conforming;
        this.validationReport = validationReport;
    }
}

