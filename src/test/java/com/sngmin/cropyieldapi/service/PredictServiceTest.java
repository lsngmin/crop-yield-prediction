package com.sngmin.cropyieldapi.service;

import ai.onnxruntime.*;
import com.sngmin.cropyieldapi.config.ModelProperties;
import com.sngmin.cropyieldapi.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sngmin.cropyieldapi.metrics.InferenceMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PredictServiceTest {
    @Mock
    OrtEnvironment env;
    @Mock
    OrtSession session;
    @Mock
    OrtSession.Result result;
    @Mock
    OnnxValue onnxValue;
    @Mock
    OnnxTensor mockTensor;

    PredictService service;

    @BeforeEach
    void setUp() throws Exception {
        FeatureMetadata metadata = new FeatureMetadata(
                List.of(
                        "Rainfall_mm",
                        "Temperature_Celsius",
                        "Fertilizer_Used",
                        "Irrigation_Used",
                        "Days_to_Harvest",
                        "Crop_Barley",
                        "Crop_Cotton",
                        "Crop_Maize",
                        "Crop_Rice",
                        "Crop_Soybean",
                        "Crop_Wheat",
                        "Region_East",
                        "Region_North",
                        "Region_South",
                        "Region_West",
                        "Soil_Type_Chalky",
                        "Soil_Type_Clay",
                        "Soil_Type_Loam",
                        "Soil_Type_Peaty",
                        "Soil_Type_Sandy",
                        "Soil_Type_Silt",
                        "Weather_Condition_Cloudy",
                        "Weather_Condition_Rainy",
                        "Weather_Condition_Sunny"
                ),
                Map.of(
                        "Crop", List.of("Barley", "Cotton", "Maize", "Rice", "Soybean", "Wheat"),
                        "Region", List.of("East", "North", "South", "West"),
                        "Soil_Type", List.of("Chalky", "Clay", "Loam", "Peaty", "Sandy", "Silt"),
                        "Weather_Condition", List.of("Cloudy", "Rainy", "Sunny")
                )
        );
        ModelProperties props = new ModelProperties("dummy", "dummy", "v1.0");

        when(session.getInputNames()).thenReturn(Set.of("X"));

        FeatureEncoder encoder = new FeatureEncoder(metadata);
        InferenceMetrics metrics = new InferenceMetrics(new SimpleMeterRegistry());

        service = new PredictService(env, session, metadata, encoder, props, metrics);
        service.init();
    }

    @Test
    @DisplayName("정상 케이스: 유효 입력 → predictedYield 반환")
    void predict_normal() throws Exception {

        when(session.run(any(Map.class))).thenReturn(result);
        when(result.get(0)).thenReturn(onnxValue);
        when(onnxValue.getValue()).thenReturn(new float[][]{{5.5f}});

        try (MockedStatic<OnnxTensor> mocked = mockStatic(OnnxTensor.class)) {
            mocked.when(() -> OnnxTensor.createTensor(any(), any(float[][].class)))
                    .thenReturn(mockTensor);

            PredictRequest req = new PredictRequest(
                    500.0, 25.0, true, true, 120,
                    "Wheat", "South", "Loam", "Rainy"
            );

            PredictResponse resp = service.predict(req);

            assertEquals(5.5, resp.predictedYield(), 0.001);
            assertEquals("v1.0", resp.modelVersion());
        }
    }

    @Test
    @DisplayName("예외 케이스: 잘못된 Crop → IllegalArgumentException")
    void predict_invalidCrop() {
        PredictRequest req = new PredictRequest(
                500.0, 25.0, true, true, 120,
                "Banana", "South", "Loam", "Rainy"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.predict(req)
        );

        assertTrue(ex.getMessage().contains("Crop"));
        assertTrue(ex.getMessage().contains("Banana"));
    }
}
