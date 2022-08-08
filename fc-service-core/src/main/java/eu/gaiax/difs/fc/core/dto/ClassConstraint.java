package eu.gaiax.difs.fc.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassConstraint {
    private final String prefix;
    private final Object value;

    public ClassConstraint(String prefix, Object value) {
        this.prefix = prefix;
        this.value = value;
    }

}
