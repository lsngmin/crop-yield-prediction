package com.sngmin.cropyieldapi.controller;

import com.sngmin.cropyieldapi.service.PredictService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PredictController.class)
class PredictControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PredictService predictService;

    @Test
    @DisplayName("Validation: 필수 필드 누락 → 400")
    void predict_missingFields_returns400() throws Exception {
        String invalidJson = "{\"rainfallMm\": 500.0}";

        mockMvc.perform(post("/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}