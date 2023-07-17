package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.PropertyResolver;

import java.util.Arrays;

public class CombinedMessageVersionConverter implements IStringConverter<CombinedMessageVersion> {
    @Override
    public CombinedMessageVersion convert(String value) {
        try {
            String resolvedValue = PropertyResolver.resolve(value);
            return CombinedMessageVersion.valueOf(resolvedValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ParameterException("Combined Message version " + value
                + " is not supported. Available versions are: "
                + Arrays.toString(CombinedMessageVersion.values()));
        }
    }
}
