package eu.gaiax.difs.fc.core.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConstraintOption {
    private final String key;
    private final Object value;

    public ConstraintOption(String key, Object value) {
        this.key = key;
        this.value = value;
    }

}
