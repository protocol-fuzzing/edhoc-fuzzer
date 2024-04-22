package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.*;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderTransformer;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.util.Arrays;

public class AlphabetTransformerRA extends AlphabetBuilderTransformer<SymbolXml, ParameterizedSymbol> {
    public AlphabetTransformerRA(AlphabetBuilderStandard<SymbolXml> alphabetBuilderStandard) {
        super(alphabetBuilderStandard);
    }

    @Override
    public SymbolXml fromTransformedInput(ParameterizedSymbol symbol) {
        DataTypeXml[] types = Arrays.stream(symbol.getPtypes())
                .map(type -> new DataTypeXml(type.getName(), type.getBase())).toArray(DataTypeXml[]::new);

        if (symbol instanceof InputSymbol) {
            return new SymbolXml(symbol.getName(), SymbolXml.Type.INPUT, types);
        } else if (symbol instanceof OutputSymbol) {
            return new SymbolXml(symbol.getName(), SymbolXml.Type.OUTPUT, types);
        } else {
            throw new RuntimeException("Invalid type for ParameterizedSymbol, not InputSymbol or OutputSymbol");
        }
    }

    @Override
    public ParameterizedSymbol toTransformedInput(SymbolXml symbol) {
        // DataType[] types = Arrays.stream(symbol.getDataTypes())
        // .map(type -> new DataType(type.getName(), type.getBase()))
        // .toArray(DataType[]::new);

        switch (symbol.getSymbolType()) {
            case INPUT:
                return new InputSymbol(symbol.getName());

            case OUTPUT:
                return new OutputSymbol(symbol.getName());

            default:
                throw new RuntimeException("Invalid SymbolType for SymbolXml, not INPUT or OUTPUT");
        }
    }
}
