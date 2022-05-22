package gr.ntua.softlab.edhocFuzzer;

import gr.ntua.softlab.edhocFuzzer.learner.EdhocAlphabetPojoXml;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilderStandard;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetSerializerException;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.xml.AlphabetSerializerXml;
import gr.ntua.softlab.protocolStateFuzzer.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.*;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerClientConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerConfigBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.WrappedSulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.TestRunner;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.TestRunnerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.TimingProbe;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.TimingProbeBuilder;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeEnabler;
import net.automatalib.words.Alphabet;

import java.io.FileNotFoundException;

public class MultiBuilder implements StateFuzzerBuilder, StateFuzzerConfigBuilder, TestRunnerBuilder, TimingProbeBuilder {

    private AlphabetBuilder alphabetBuilder = new AlphabetBuilderStandard(new AlphabetSerializerXml<>(EdhocAlphabetPojoXml.class));

    private MapperBuilder mapperBuilder = null;

    private WrappedSulBuilder wrappedSulBuilder = null;

    @Override
    public StateFuzzerClientConfig buildClientConfig() {
        Alphabet<AbstractInput> alphabet = alphabetBuilder.build(()->null);
        try {
            alphabetBuilder.exportAlphabetToFile("alphabetTest.xml", alphabet);
        } catch (FileNotFoundException | AlphabetSerializerException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public StateFuzzerServerConfig buildServerConfig() {
        return null;
    }

    @Override
    public StateFuzzer build(StateFuzzerConfig stateFuzzerConfig) {
        StateFuzzerComposer stateFuzzerComposer = new StateFuzzerComposerStandard(stateFuzzerConfig,
                alphabetBuilder, mapperBuilder, wrappedSulBuilder);
        return new StateFuzzerStandard(stateFuzzerComposer);
    }

    @Override
    public TestRunner build(TestRunnerEnabler testRunnerEnabler) {
        return null;
    }

    @Override
    public TimingProbe build(TimingProbeEnabler timingProbeEnabler) {
        return null;
    }
}
