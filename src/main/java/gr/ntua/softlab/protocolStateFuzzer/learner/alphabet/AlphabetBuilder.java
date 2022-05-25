package gr.ntua.softlab.protocolStateFuzzer.learner.alphabet;

import gr.ntua.softlab.protocolStateFuzzer.learner.config.AlphabetOptionProvider;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import net.automatalib.words.Alphabet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

public interface AlphabetBuilder {
    String DEFAULT_ALPHABET = "default_alphabet.xml";

    // TODO this probably doesn't work when executing via .jar
    static File getAlphabetFile(AlphabetOptionProvider config) {
        if (config.getAlphabet() != null) {
            return new File(config.getAlphabet());
        } else {
            return new File(Objects.requireNonNull(AlphabetBuilder.class.getClassLoader()
                    .getResource(DEFAULT_ALPHABET)).getFile());
        }
    }

    Alphabet<AbstractInput> build(AlphabetOptionProvider config);

    void exportAlphabetToFile(String outputFileName, Alphabet<AbstractInput> alphabet) throws FileNotFoundException,
            AlphabetSerializerException;
}
