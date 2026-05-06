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
import com.sngmin.cropyieldapi.metrics.InferenceMetrics;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictService {
    private final OrtEnvironment env;
    private final OrtSession session;
    private final FeatureMetadata metadata;
    private final FeatureEncoder encoder;
    private final ModelProperties props;
    private final InferenceMetrics metrics;

    private String inputName;

    @PostConstruct
    void init() {
        this.inputName = session.getInputNames().iterator().next();
    }

    public PredictResponse predict(PredictRequest req) {
        metrics.incrementRequest();

        try {
            log.info("Predict request: crop={}, region={}", req.crop(), req.region());

            validateCategoricals(req);
            float[] features = encoder.encode(req);

            // 추론 시간만 측정 (검증/인코딩 시간 제외)
            float prediction = metrics.inferenceTimer().record(
                    (Supplier<Float>) () -> runInference(features)
            );

            metrics.incrementSuccess();
            log.info("Prediction: {} ton/ha", prediction);

            return new PredictResponse((double) prediction, props.version());
        } catch (RuntimeException e) {
            metrics.incrementError();
            throw e;
        }
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

    private float runInference(float[] features) {
        try {
            float[][] input = new float[][]{ features };

            try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
                 OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
                float[][] output = (float[][]) result.get(0).getValue();
                return output[0][0];
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed", e);
        }
    }
}