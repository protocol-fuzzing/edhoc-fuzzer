package gr.ntua.softlab.protocolStateFuzzer.sul.config;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class ProtocolVersionConverter implements IStringConverter<ProtocolVersion> {

    @Override
    public ProtocolVersion convert(String value) {
        try {
            return ProtocolVersion.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Value " + value
                    + " cannot be converted to a ProtocolVersion. "
                    + "Available values are: "
                    + String.join(", ", ProtocolVersion.names()));
        }
    }
}
