package com.konrad.konradquiz.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${app.encryption.secret-key}")
    private String secretKey;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        config.setPassword(secretKey);
        config.setAlgorithm("PBEWithHMACSHA512AndAES_256"); // industry standard
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("2");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator"); // random salt per encryption ✅
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");       // random IV per encryption ✅
        config.setStringOutputType("base64");

        encryptor.setConfig(config);
        return encryptor;
    }
}