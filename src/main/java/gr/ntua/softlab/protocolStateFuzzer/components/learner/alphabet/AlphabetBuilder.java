package gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.AlphabetOptionProvider;
import net.automatalib.words.Alphabet;

import java.io.FileNotFoundException;

public interface AlphabetBuilder {
    String DEFAULT_ALPHABET_NO_EXTENSION = "default_alphabet";

    Alphabet<AbstractInput> build(AlphabetOptionProvider config);

    String getAlphabetFileName(AlphabetOptionProvider config);

    String getAlphabetFileExtension();

    void exportAlphabetToFile(String outputFileName, Alphabet<AbstractInput> alphabet) throws FileNotFoundException,
            AlphabetSerializerException;
}
