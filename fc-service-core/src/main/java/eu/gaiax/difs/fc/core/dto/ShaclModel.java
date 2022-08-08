package eu.gaiax.difs.fc.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShaclModel {

    private final List<Map<String, String>> prefixList;
    private final List<VicShape> shapes;

    public ShaclModel(List<Map<String, String>> prefixList, List<VicShape> shapes) {
        this.prefixList = prefixList;
        this.shapes = shapes;
    }

}