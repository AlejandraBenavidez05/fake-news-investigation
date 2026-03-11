
package com.konrad.konradquiz.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Converter
@Component
public class StringEncryptionConverter
        implements AttributeConverter<String, String>, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "Intentional: ApplicationContextAware pattern required for JPA AttributeConverter — " +
                    "Spring guarantees single context, static field is safe here"
    )
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        StringEncryptionConverter.applicationContext = ctx;
    }

    private StringEncryptor encryptor() {
        return applicationContext.getBean("jasyptStringEncryptor", StringEncryptor.class);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return encryptor().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return encryptor().decrypt(dbData);
    }
}