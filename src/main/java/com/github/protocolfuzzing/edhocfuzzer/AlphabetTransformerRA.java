package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.*;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderTransformer;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.InputSymbol;

import java.util.Arrays;

public class AlphabetTransformerRA extends AlphabetBuilderTransformer<InputSymbolXml, EdhocInputRA> {
    public AlphabetTransformerRA(AlphabetBuilderStandard<InputSymbolXml> alphabetBuilderStandard) {
        super(alphabetBuilderStandard);
    }

    @Override
    public InputSymbolXml fromTransformedInput(EdhocInputRA input) {
        DataTypeXml[] types = Arrays.stream(input.getDataTypes())
                .map(type -> new DataTypeXml(type.getName(), type.getBase())).toArray(DataTypeXml[]::new);
        return new InputSymbolXml(input.getName(), types);
    }

    @Override
    public EdhocInputRA toTransformedInput(InputSymbolXml input) {
        DataType[] types = Arrays.stream(input.getDataTypes()).map(type -> new DataType(type.getName(), type.getBase()))
                .toArray(DataType[]::new);
        InputSymbol base = new InputSymbol(input.getName(), types);

        if (input.getName() == null) {
            throw new RuntimeException("Nameless symbol found, possibly due to malformed alphabet file");
        }

        switch (MessageInputType.valueOf(input.getName())) {
            case EDHOC_MESSAGE_1:
                return new EdhocMessage1InputRA(base, null);

            case EDHOC_MESSAGE_2:
                return new EdhocMessage2InputRA(base, null);

            case EDHOC_MESSAGE_3:
                return new EdhocMessage3InputRA(base, null);

            case EDHOC_MESSAGE_3_OSCORE_APP:
                return new EdhocMessage3OscoreAppInputRA(base, null);

            case EDHOC_MESSAGE_4:
                return new EdhocMessage4InputRA(base, null);

            case EDHOC_ERROR_MESSAGE:
                return new EdhocErrorMessageInputRA(base, null);

            case OSCORE_APP_MESSAGE:
                return new OscoreAppMessageInputRA(base, null);

            case COAP_APP_MESSAGE:
                return new CoapAppMessageInputRA(base, null);

            case COAP_EMPTY_MESSAGE:
                return new CoapEmptyMessageInputRA(base, null);

            default:
                throw new RuntimeException("Invalid input type, no such input type exists");
        }

    }
}
