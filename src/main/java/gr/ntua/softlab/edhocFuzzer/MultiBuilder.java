package gr.ntua.softlab.edhocFuzzer;

import gr.ntua.softlab.edhocFuzzer.learner.EdhocAlphabetPojoXml;
import gr.ntua.softlab.edhocFuzzer.sul.EdhocSulClientConfig;
import gr.ntua.softlab.edhocFuzzer.sul.EdhocSulServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilderStandard;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.xml.AlphabetSerializerXml;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.StateFuzzer;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.StateFuzzerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.StateFuzzerComposerStandard;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.StateFuzzerStandard;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.*;
import gr.ntua.softlab.protocolStateFuzzer.sul.WrappedSulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.TestRunner;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.TestRunnerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.TimingProbe;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.TimingProbeBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

public class MultiBuilder implements StateFuzzerConfigBuilder, StateFuzzerBuilder, TestRunnerBuilder, TimingProbeBuilder {

    private AlphabetBuilder alphabetBuilder = new AlphabetBuilderStandard(
            new AlphabetSerializerXml<>(EdhocAlphabetPojoXml.class)
    );

    private MapperBuilder mapperBuilder = null;

    private WrappedSulBuilder wrappedSulBuilder = null;

    @Override
    public StateFuzzerClientConfig buildClientConfig() {
        return new StateFuzzerClientConfig(
                new LearnerConfig(),
                new MapperConfig(),
                new TestRunnerConfig(),
                new TimingProbeConfig(),
                new EdhocSulServerConfig()
        );
    }

    @Override
    public StateFuzzerServerConfig buildServerConfig() {
        return new StateFuzzerServerConfig(
                new LearnerConfig(),
                new MapperConfig(),
                new TestRunnerConfig(),
                new TimingProbeConfig(),
                new EdhocSulClientConfig()
        );
    }

    @Override
    public StateFuzzer build(StateFuzzerEnabler stateFuzzerEnabler) {
        return new StateFuzzerStandard(
                new StateFuzzerComposerStandard(stateFuzzerEnabler, alphabetBuilder, mapperBuilder, wrappedSulBuilder)
        );
    }

    @Override
    public TestRunner build(TestRunnerEnabler testRunnerEnabler) {
        return new TestRunner(testRunnerEnabler, alphabetBuilder, mapperBuilder, wrappedSulBuilder);
    }

    @Override
    public TimingProbe build(TimingProbeEnabler timingProbeEnabler) {
        return new TimingProbe(timingProbeEnabler, alphabetBuilder, mapperBuilder, wrappedSulBuilder);
    }
}
