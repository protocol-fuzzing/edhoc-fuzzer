package gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.AlphabetOptionProvider;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import net.automatalib.words.Alphabet;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface AlphabetBuilder {
    String DEFAULT_ALPHABET_NO_EXTENSION = "default_alphabet";

    Alphabet<AbstractInput> build(AlphabetOptionProvider config);

    InputStream getAlphabetFileInputStream(AlphabetOptionProvider config);

    String getAlphabetFileExtension();

    void exportAlphabetToFile(String outputFileName, Alphabet<AbstractInput> alphabet) throws FileNotFoundException,
            AlphabetSerializerException;
}
