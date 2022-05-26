package gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.AlphabetOptionProvider;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import net.automatalib.words.Alphabet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AlphabetBuilderStandard implements AlphabetBuilder {
    private static final Logger LOGGER = LogManager.getLogger(AlphabetBuilderStandard.class);
    protected String DEFAULT_ALPHABET;
    protected AlphabetSerializer alphabetSerializer;

    // store already built maps, so as not to rebuild them if needed
    protected Map<AlphabetOptionProvider, Alphabet<AbstractInput>> builtMap = new LinkedHashMap<>();

    public AlphabetBuilderStandard(AlphabetSerializer alphabetSerializer) {
        this.alphabetSerializer = alphabetSerializer;
        this.DEFAULT_ALPHABET = DEFAULT_ALPHABET_NO_EXTENSION + alphabetSerializer.getAlphabetFileExtension();
    }

    @Override
    public Alphabet<AbstractInput> build(AlphabetOptionProvider config) {
        if (builtMap.containsKey(config)) {
            return builtMap.get(config);
        }

        Alphabet<AbstractInput> alphabet;
        if (config.getAlphabet() != null) {
            try {
                alphabet = buildConfiguredAlphabet(config);
            } catch (AlphabetSerializerException | FileNotFoundException e) {
                LOGGER.fatal("Failed to instantiate provided alphabet");
                throw new RuntimeException(e);
            }
        } else {
            try {
                alphabet = buildDefaultAlphabet();
            } catch (AlphabetSerializerException e) {
                LOGGER.fatal("Failed to instantiate default alphabet");
                throw new RuntimeException(e);
            }
        }

        builtMap.put(config, alphabet);
        return alphabet;
    }

    protected Alphabet<AbstractInput> buildConfiguredAlphabet(AlphabetOptionProvider config)
            throws AlphabetSerializerException, FileNotFoundException {
        Alphabet<AbstractInput> alphabet = null;
        if (config.getAlphabet() != null) {
            alphabet = alphabetSerializer.read(new FileInputStream(config.getAlphabet()));
        }
        return alphabet;
    }

    protected Alphabet<AbstractInput> buildDefaultAlphabet() throws AlphabetSerializerException {
        return alphabetSerializer.read(this.getClass().getClassLoader().getResourceAsStream(DEFAULT_ALPHABET));
    }

    @Override
    public String getAlphabetFileName(AlphabetOptionProvider config) {
        if (config.getAlphabet() != null) {
            return config.getAlphabet();
        } else {
            return Objects.requireNonNull(this.getClass().getClassLoader().getResource(DEFAULT_ALPHABET)).getPath();
        }
    }

    @Override
    public String getAlphabetFileExtension() {
        return alphabetSerializer.getAlphabetFileExtension();
    }

    @Override
    public void exportAlphabetToFile(String outputFileName, Alphabet<AbstractInput> alphabet)
            throws FileNotFoundException, AlphabetSerializerException {
        FileOutputStream alphabetStream = new FileOutputStream(outputFileName);
        alphabetSerializer.write(alphabetStream, alphabet);
    }
}
