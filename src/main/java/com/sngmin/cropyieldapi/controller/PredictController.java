package com.sngmin.cropyieldapi.controller;

import com.sngmin.cropyieldapi.model.PredictRequest;
import com.sngmin.cropyieldapi.model.PredictResponse;
import com.sngmin.cropyieldapi.service.PredictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PredictController {

    private final PredictService predictService;

    @PostMapping("/predict")
    public PredictResponse predict(@Valid @RequestBody PredictRequest request) {
        log.info("Received predict request");
        return predictService.predict(request);
    }
}