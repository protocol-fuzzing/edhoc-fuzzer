package gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import net.automatalib.words.Alphabet;

import java.io.InputStream;
import java.io.OutputStream;

public interface AlphabetSerializer {
    Alphabet<AbstractInput> read(InputStream alphabetStream) throws AlphabetSerializerException;
    void write(OutputStream alphabetStream, Alphabet<AbstractInput> alphabet) throws AlphabetSerializerException;
}
