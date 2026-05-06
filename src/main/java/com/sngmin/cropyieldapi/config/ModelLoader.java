package com.sngmin.cropyieldapi.config;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.sngmin.cropyieldapi.model.FeatureMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.Model;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

@Configuration
public class ModelLoader {
    private final ModelProperties props;

    public ModelLoader(ModelProperties props) {
        this.props = props;
    }

    @Bean
    public OrtEnvironment ortEnvironment() {
        return OrtEnvironment.getEnvironment();
    }

    @Bean
    public OrtSession ortSession(OrtEnvironment env) throws OrtException {
        return env.createSession(props.onnxPath());
    }

    @Bean
    public FeatureMetadata featureMetadata() throws IOException {
        return new ObjectMapper().readValue(
                new File(props.metadataPath()),
                FeatureMetadata.class
        );
    }
}
