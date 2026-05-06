package com.sngmin.cropyieldapi.model;

public record PredictResponse(
        Double predictedYield,
        String modelVersion
) {
}
