package com.sngmin.cropyieldapi.service;

import com.sngmin.cropyieldapi.model.FeatureMetadata;
import com.sngmin.cropyieldapi.model.PredictRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FeatureEncoder {

    private final FeatureMetadata metadata;

    public float[] encode(PredictRequest req) {
        // 1단계: 컬럼명 → 값 매핑 빌드
        Map<String, Float> valueMap = new HashMap<>();

        valueMap.put("Rainfall_mm", req.rainfallMm().floatValue());
        valueMap.put("Temperature_Celsius", req.temperatureCelsius().floatValue());
        valueMap.put("Fertilizer_Used", req.fertilizerUsed() ? 1f : 0f);
        valueMap.put("Irrigation_Used", req.irrigationUsed() ? 1f : 0f);
        valueMap.put("Days_to_Harvest", req.daysToHarvest().floatValue());

        // 2단계: 원핫 인코딩 (해당 카테고리 컬럼만 1, 나머지 0)
        valueMap.put("Crop_" + req.crop(), 1f);
        valueMap.put("Region_" + req.region(), 1f);
        valueMap.put("Soil_Type_" + req.soilType(), 1f);
        valueMap.put("Weather_Condition_" + req.weatherCondition(), 1f);

        // 3단계: featureOrder 순서대로 배열 만들기 (없는 컬럼은 0)
        List<String> order = metadata.featureOrder();
        float[] features = new float[order.size()];
        for (int i = 0; i < order.size(); i++) {
            features[i] = valueMap.getOrDefault(order.get(i), 0f);
        }
        return features;
    }
}