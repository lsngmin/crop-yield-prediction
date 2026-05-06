package com.sngmin.cropyieldapi.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PredictRequest(
        @NotNull @Min(0)
        Double rainfallMm,

        @NotNull
        Double temperatureCelsius,

        @NotNull
        Boolean fertilizerUsed,

        @NotNull
        Boolean irrigationUsed,

        @NotNull @Min(1)
        Integer daysToHarvest,

        @NotBlank
        String crop,

        @NotBlank
        String region,

        @NotBlank
        String soilType,

        @NotBlank
        String weatherCondition
) {}