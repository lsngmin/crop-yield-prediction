package com.sngmin.cropyieldapi.model;

import java.util.List;

public record BatchPredictResponse(
        List<PredictResponse> predictions,
        String modelVersion,
        int count
) {}