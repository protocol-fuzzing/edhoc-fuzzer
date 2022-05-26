package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class ProtocolVersionConverter implements IStringConverter<ProtocolVersion> {

    @Override
    public ProtocolVersion convert(String value) {
        try {
            return ProtocolVersion.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Protocol version " + value + " is not supported. Available versions are: "
                    + String.join(", ", ProtocolVersion.names()));
        }
    }
}
