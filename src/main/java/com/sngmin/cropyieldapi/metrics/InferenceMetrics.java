package com.sngmin.cropyieldapi.metrics;

import lombok.Getter;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component

public class InferenceMetrics {
    private final Counter requestCounter;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer inferenceTimer;

    public InferenceMetrics(MeterRegistry registry) {
        this.requestCounter = Counter.builder("crop_yield_predict_requests_total")
                .description("Total number of predict requests")
                .register(registry);

        this.successCounter = Counter.builder("crop_yield_predict_success_total")
                .description("Total number of successful predict requests")
                .register(registry);

        this.errorCounter = Counter.builder("crop_yield_predict_errors_total")
                .description("Total number of failed predict requests")
                .register(registry);

        this.inferenceTimer = Timer.builder("crop_yield_inference_duration")
                .description("ONNX inference duration")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void incrementRequest() {
        requestCounter.increment();
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

    public Timer inferenceTimer() {
        return inferenceTimer;
    }
}
