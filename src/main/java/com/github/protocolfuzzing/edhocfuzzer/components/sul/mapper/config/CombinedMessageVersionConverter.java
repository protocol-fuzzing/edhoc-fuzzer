package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.PropertyResolver;

import java.util.Arrays;

public class CombinedMessageVersionConverter implements IStringConverter<CombinedMessageVersion> {
    @Override
    public CombinedMessageVersion convert(String value) {
        if (value == null) {
            throw new ParameterException("Provided null Combined Message version value");
        }

        try {
            String resolvedValue = PropertyResolver.resolve(value);
            return CombinedMessageVersion.valueOf(resolvedValue);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Combined Message version " + value
                + " is not supported. Available versions are: "
                + Arrays.toString(CombinedMessageVersion.values()));
        }
    }
}
