package com.sngmin.cropyieldapi.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchPredictRequest(
        @NotEmpty
        @Size(max = 100)
        List<@Valid PredictRequest> items
) {}