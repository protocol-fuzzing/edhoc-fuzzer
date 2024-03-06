package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.*;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.InputSymbolXml;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.MessageInputType;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderTransformer;
import de.learnlib.ralib.words.InputSymbol;

public class AlphabetTransformerRA extends AlphabetBuilderTransformer<InputSymbolXml, EdhocInputRA> {
    public AlphabetTransformerRA(AlphabetBuilderStandard<InputSymbolXml> alphabetBuilderStandard) {
        super(alphabetBuilderStandard);
    }

    @Override
    public InputSymbolXml fromTransformedInput(EdhocInputRA input) {
        InputSymbolXml result = new InputSymbolXml();
        result.setName(input.getBaseSymbol().getName());
        result.setDataTypes(input.getBaseSymbol().getPtypes());
        return result;
    }

    @Override
    public EdhocInputRA toTransformedInput(InputSymbolXml input) {
        InputSymbol base = new InputSymbol(input.getName(), input.getDataTypes());
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
