package com.sngmin.cropyieldapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.model")
public record ModelProperties (
    String onnxPath,
    String metadataPath,
    String version
) {}

