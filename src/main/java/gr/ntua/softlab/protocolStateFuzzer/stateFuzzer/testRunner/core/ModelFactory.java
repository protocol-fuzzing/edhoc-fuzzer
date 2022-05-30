package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.NameToAbstractSymbol;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractIOStringProcessor;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.serialization.InputModelData;
import net.automatalib.words.Alphabet;

import java.io.FileInputStream;
import java.io.IOException;

public class ModelFactory {
    public static MealyMachine<?, AbstractInput, ?, AbstractOutput> buildProtocolModel(
            Alphabet<AbstractInput> alphabet, String modelPath
    ) throws IOException {
        NameToAbstractSymbol<AbstractInput> definitions = new NameToAbstractSymbol<>(alphabet);
        InputModelData<AbstractInput, CompactMealy<AbstractInput, AbstractOutput>> result = MealyDotParser.parse(
                new CompactMealy.Creator<>(), new FileInputStream(modelPath), new AbstractIOStringProcessor(definitions)
        );
        return result.model;
    }
}
