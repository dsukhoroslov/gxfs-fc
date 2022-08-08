package eu.gaiax.difs.fc.core.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VicShape {
    private final String schema;
    private final String targetClassPrefix;
    private final String targetClassName;
    private final List<ShapeProperties> constraints;

    public VicShape(List<ShapeProperties> constraints, String schema, String targetClassPrefix, String targetClassName) {
        this.targetClassPrefix = targetClassPrefix;
        this.constraints = (null == constraints || constraints.isEmpty() ? null : new ArrayList<>(constraints));
        this.schema = schema;
        this.targetClassName = targetClassName;
    }
}
