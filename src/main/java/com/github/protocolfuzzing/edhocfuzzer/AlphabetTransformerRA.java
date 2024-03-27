package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.*;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.*;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderTransformer;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.util.Arrays;

public class AlphabetTransformerRA extends AlphabetBuilderTransformer<SymbolXml, PSymbolInstance> {
    public AlphabetTransformerRA(AlphabetBuilderStandard<SymbolXml> alphabetBuilderStandard) {
        super(alphabetBuilderStandard);
    }

    @Override
    public SymbolXml fromTransformedInput(PSymbolInstance symbol) {
        ParameterizedSymbol baseSymbol = symbol.getBaseSymbol();

        DataTypeXml[] types = Arrays.stream(baseSymbol.getPtypes())
                .map(type -> new DataTypeXml(type.getName(), type.getBase())).toArray(DataTypeXml[]::new);

        if (symbol.getBaseSymbol() instanceof InputSymbol) {
            return new SymbolXml(baseSymbol.getName(), SymbolXml.Type.INPUT, types);
        } else if (symbol.getBaseSymbol() instanceof OutputSymbol) {
            return new SymbolXml(baseSymbol.getName(), SymbolXml.Type.OUTPUT, types);
        } else {
            throw new RuntimeException("Invalid type for ParameterizedSymbol, not InputSymbol or OutputSymbol");
        }
    }

    @Override
    public PSymbolInstance toTransformedInput(SymbolXml symbol) {
        if (symbol.getName() == null) {
            throw new RuntimeException("Nameless symbol found, possibly due to malformed alphabet file");
        }

        ParameterizedSymbol baseSymbol = createBaseSymbol(symbol);
        switch (symbol.getSymbolType()) {
            case INPUT:
                return createInputSymbol(baseSymbol);

            case OUTPUT:
                return createOutputSymbol(baseSymbol);

            default:
                throw new RuntimeException("Invalid SymbolType for InputSymbolXml, not INPUT or OUTPUT");
        }

    }

    private static ParameterizedSymbol createBaseSymbol(SymbolXml symbol) {
        DataType[] types = Arrays.stream(symbol.getDataTypes())
                .map(type -> new DataType(type.getName(), type.getBase()))
                .toArray(DataType[]::new);

        switch (symbol.getSymbolType()) {
            case INPUT:
                return new InputSymbol(symbol.getName(), types);

            case OUTPUT:
                return new OutputSymbol(symbol.getName(), types);

            default:
                throw new RuntimeException("Invalid SymbolType for InputSymbolXml, not INPUT or OUTPUT");
        }
    }

    private static EdhocOutputRA createOutputSymbol(ParameterizedSymbol baseSymbol) {
        return new EdhocOutputRA(baseSymbol, new DataValue<?>[] {});
    }

    private static EdhocInputRA createInputSymbol(ParameterizedSymbol baseSymbol) {
        switch (MessageInputType.valueOf(baseSymbol.getName())) {
            case EDHOC_MESSAGE_1:
                return new EdhocMessage1InputRA(baseSymbol, new DataValue<?>[] {});

            case EDHOC_MESSAGE_2:
                return new EdhocMessage2InputRA(baseSymbol, new DataValue<?>[] {});

            case EDHOC_MESSAGE_3:
                return new EdhocMessage3InputRA(baseSymbol, new DataValue<?>[] {});

            case EDHOC_MESSAGE_3_OSCORE_APP:
                return new EdhocMessage3OscoreAppInputRA(baseSymbol, new DataValue<?>[] {});

            case EDHOC_MESSAGE_4:
                return new EdhocMessage4InputRA(baseSymbol, new DataValue<?>[] {});

            case EDHOC_ERROR_MESSAGE:
                return new EdhocErrorMessageInputRA(baseSymbol, new DataValue<?>[] {});

            case OSCORE_APP_MESSAGE:
                return new OscoreAppMessageInputRA(baseSymbol, new DataValue<?>[] {});

            case COAP_APP_MESSAGE:
                return new CoapAppMessageInputRA(baseSymbol, new DataValue<?>[] {});

            case COAP_EMPTY_MESSAGE:
                return new CoapEmptyMessageInputRA(baseSymbol, new DataValue<?>[] {});

            default:
                throw new RuntimeException("Invalid input type, no such input type exists");
        }
    }
}
