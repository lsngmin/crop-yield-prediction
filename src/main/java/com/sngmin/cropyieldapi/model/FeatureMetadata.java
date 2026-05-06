package com.sngmin.cropyieldapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FeatureMetadata(
        @JsonProperty("feature_order") List<String> featureOrder,
        @JsonProperty("categorical_values") Map<String, List<String>> categoricalValues
) {
}
