package gr.ntua.softlab.protocolStateFuzzer.learner.alphabet;

import gr.ntua.softlab.protocolStateFuzzer.learner.config.AlphabetOptionProvider;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import net.automatalib.words.Alphabet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class AlphabetBuilderStandard implements AlphabetBuilder {
    private static final Logger LOGGER = LogManager.getLogger(AlphabetBuilderStandard.class);

    protected AlphabetSerializer alphabetSerializer;

    // store already built maps, so as not to rebuild them if needed
    protected Map<AlphabetOptionProvider, Alphabet<AbstractInput>> builtMap = new LinkedHashMap<>();

    public AlphabetBuilderStandard(AlphabetSerializer alphabetSerializer) {
        this.alphabetSerializer = alphabetSerializer;
    }

    @Override
    public Alphabet<AbstractInput> build(AlphabetOptionProvider config) {
        if (builtMap.containsKey(config)) {
            return builtMap.get(config);
        }

        Alphabet<AbstractInput> alphabet = null;
        if (config.getAlphabet() != null) {
            try {
                alphabet = buildConfiguredAlphabet(config);
            } catch (AlphabetSerializerException | FileNotFoundException e) {
                LOGGER.fatal("Failed to instantiate alphabet");
                LOGGER.fatal(e.getMessage());
                System.exit(1);
            }
        } else {
            try {
                alphabet = buildDefaultAlphabet();
            } catch (AlphabetSerializerException e) {
                LOGGER.fatal("Failed to instantiate default alphabet");
                LOGGER.fatal(e.getMessage());
                System.exit(1);
            }
        }

        builtMap.put(config, alphabet);
        return alphabet;
    }


    protected Alphabet<AbstractInput> buildDefaultAlphabet() throws AlphabetSerializerException {
        return alphabetSerializer.read(AlphabetBuilderStandard.class.getClassLoader().getResourceAsStream(DEFAULT_ALPHABET));
    }

    protected Alphabet<AbstractInput> buildConfiguredAlphabet(AlphabetOptionProvider config)
            throws AlphabetSerializerException, FileNotFoundException {
        Alphabet<AbstractInput> alphabet = null;
        if (config.getAlphabet() != null) {
            alphabet = alphabetSerializer.read(new FileInputStream(config.getAlphabet()));
        }
        return alphabet;
    }

    @Override
    public void exportAlphabetToFile(String outputFileName, Alphabet<AbstractInput> alphabet)
            throws FileNotFoundException, AlphabetSerializerException {
        if (outputFileName != null) {
            FileOutputStream alphabetStream = new FileOutputStream(outputFileName);
            alphabetSerializer.write(alphabetStream, alphabet);
        }
    }
}
