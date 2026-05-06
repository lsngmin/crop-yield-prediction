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
import com.sngmin.cropyieldapi.model.BatchPredictRequest;
import com.sngmin.cropyieldapi.model.BatchPredictResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

            float prediction = (float) metrics.inferenceTimer().record(() -> runInference(features));

            metrics.incrementSuccess();
            log.info("Prediction: {} ton/ha", prediction);

            return new PredictResponse((double) prediction, props.version());
        } catch (RuntimeException e) {
            metrics.incrementError();
            throw e;
        }
    }

    public BatchPredictResponse predictBatch(BatchPredictRequest batchReq) {
        metrics.incrementRequest();

        try {
            List<PredictRequest> items = batchReq.items();
            log.info("Batch predict request: count={}", items.size());

            float[][] batchFeatures = new float[items.size()][];
            for (int i = 0; i < items.size(); i++) {
                PredictRequest req = items.get(i);
                validateCategoricals(req);
                batchFeatures[i] = encoder.encode(req);
            }

            float[] predictions = metrics.inferenceTimer().record(
                    () -> runBatchInference(batchFeatures)
            );

            List<PredictResponse> responses = new ArrayList<>(predictions.length);
            for (float p : predictions) {
                responses.add(new PredictResponse((double) p, props.version()));
            }

            metrics.incrementSuccess();
            log.info("Batch prediction completed: count={}", responses.size());

            return new BatchPredictResponse(responses, props.version(), responses.size());
        } catch (RuntimeException e) {
            metrics.incrementError();
            throw e;
        }
    }

    private float[] runBatchInference(float[][] batchFeatures) {
        try {
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, batchFeatures);
                 OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
                float[][] output = (float[][]) result.get(0).getValue();
                float[] predictions = new float[output.length];
                for (int i = 0; i < output.length; i++) {
                    predictions[i] = output[i][0];
                }
                return predictions;
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX batch inference failed", e);
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