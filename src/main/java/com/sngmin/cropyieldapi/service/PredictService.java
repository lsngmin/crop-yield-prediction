package com.sngmin.cropyieldapi.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.sngmin.cropyieldapi.config.ModelProperties;
import com.sngmin.cropyieldapi.model.FeatureMetadata;
import com.sngmin.cropyieldapi.model.PredictRequest;
import com.sngmin.cropyieldapi.model.PredictResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictService {
    private final OrtEnvironment env;
    private final OrtSession session;
    private final FeatureMetadata metadata;
    private final ModelProperties props;
    private String inputName;

    @PostConstruct
    void init() {
        this.inputName = session.getInputNames().iterator().next();
    }

    public PredictResponse predict(PredictRequest req) {
        log.info("Predict request: crop={}, region={}", req.crop(), req.region());
        validateCategoricals(req);
        float[] features = encodeFeatures(req);
        float prediction = runInference(features);
        log.info("Prediction: {} ton/ha", prediction);

        return new PredictResponse((double) prediction, props.version());
    }

    private void validateCategoricals(PredictRequest req) {
        checkInList("Crop", req.crop());
        checkInList("Region", req.region());
        checkInList("Soil_Type", req.soilType());
        checkInList("Weather_Condition", req.weatherCondition());
    }

    private void checkInList(String category, String value) {
        List<String> allowed = metadata.categoricalValues().get(category);
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                    "Invalid " + category + ": " + value + ". Allowed: " + allowed
            );
        }
    }

    private float[] encodeFeatures(PredictRequest req) {
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

    private float runInference(float[] features) {
        try {
            // [1, 24] 배열로 batch 차원 추가
            float[][] input = new float[][]{ features };

            // 텐서 생성
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, input)) {
                // 추론
                try (OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
                    // 출력 shape: [1, 1] — batch 1개, 회귀 출력 1개
                    float[][] output = (float[][]) result.get(0).getValue();
                    return output[0][0];
                }
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed", e);
        }
    }
}
