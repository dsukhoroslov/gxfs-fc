package eu.gaiax.difs.fc.core.pojo;

/**
 * POJO Class for holding Semantic Validation Results.
 */
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.Setter
public class SemanticValidationResult {
    private final boolean conforms;
    private final String validationReport;

    public SemanticValidationResult(boolean conforms,String validationReport) {
        this.conforms = conforms;
        this.validationReport = validationReport;
    }


}
}
