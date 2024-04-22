package com.github.protocolfuzzing.edhocfuzzer;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.*;
import net.automatalib.alphabet.ListAlphabet;

import java.util.ArrayList;

public class EnumAlphabet extends ListAlphabet<ParameterizedSymbol> {

    private EnumAlphabet(ArrayList<ParameterizedSymbol> inputs) {
        super(inputs);
    }

    public static class Builder {

        // TODO: This being static might mean that the list is never cleared unless done
        // so specifically,
        // meaning that constructing multiple alphabets might break unless this is
        // cleared and a deepcopy
        // is passed to EnumAlphabet.
        private static ArrayList<ParameterizedSymbol> symbols = new ArrayList<>();

        public Builder() {};

        // TODO: This could be made generic like the methods for multiple enum values.
        public Builder withInput(MessageInputTypeRA inputName, DataType... dataTypes) {
            InputSymbol input = new InputSymbol(inputName.name());
            symbols.add(input);
            return this;
        }

        // TODO: This could be made generic like the methods for multiple enum values.
        public Builder withOutput(MessageOutputTypeRA outputName, DataType... dataTypes) {
            OutputSymbol output = new OutputSymbol(outputName.name(), dataTypes);
            symbols.add(output);
            return this;
        }

        public <T extends Enum<T>> Builder withInputs(T[] inputNames) {
            for (T e : inputNames) {
                symbols.add(new InputSymbol(e.name()));
            }
            return this;
        }

        public <T extends Enum<T>> Builder withOutputs(T[] outputNames) {
            for (T e : outputNames) {
                symbols.add(new OutputSymbol(e.name()));
            }
            return this;
        }

        public EnumAlphabet build() {
            // TODO: Deepcopy symbols, clear symbols after construction?
            return new EnumAlphabet(symbols);
        }

    }
}
