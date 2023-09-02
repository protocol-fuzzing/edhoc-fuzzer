package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.PropertyResolver;

import java.util.Arrays;

public class ProtocolVersionConverter implements IStringConverter<ProtocolVersion> {
    @Override
    public ProtocolVersion convert(String value) {
        if (value == null) {
            throw new ParameterException("Provided null Protocol version value");
        }

        try {
            String resolvedValue = PropertyResolver.resolve(value);
            return ProtocolVersion.valueOf(resolvedValue);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Protocol version " + value
                + " is not supported. Available versions are: "
                + Arrays.toString(ProtocolVersion.values()));
        }
    }
}
