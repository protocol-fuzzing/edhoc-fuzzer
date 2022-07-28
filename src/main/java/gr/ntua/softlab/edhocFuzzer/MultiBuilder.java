package gr.ntua.softlab.edhocFuzzer;

import gr.ntua.softlab.edhocFuzzer.components.learner.EdhocAlphabetPojoXml;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.EdhocSulBuilder;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulClientConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulServerConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet.xml.AlphabetSerializerXml;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.SulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.SulWrapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.SulWrapperStandard;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.StateFuzzer;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.StateFuzzerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.StateFuzzerComposerStandard;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.StateFuzzerStandard;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerClientConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerConfigBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.TestRunner;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.TestRunnerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.TimingProbe;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.TimingProbeBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

public class MultiBuilder implements StateFuzzerConfigBuilder, StateFuzzerBuilder, TestRunnerBuilder, TimingProbeBuilder {

    protected AlphabetBuilder alphabetBuilder = new AlphabetBuilderStandard(
            new AlphabetSerializerXml<>(EdhocAlphabetPojoXml.class)
    );

    protected SulBuilder sulBuilder = new EdhocSulBuilder();
    protected SulWrapper sulWrapper = new SulWrapperStandard();

    @Override
    public StateFuzzerClientConfig buildClientConfig() {
        return new StateFuzzerClientConfig(
                new LearnerConfig(),
                new EdhocSulClientConfig(new EdhocMapperConfig()),
                new TestRunnerConfig(),
                new TimingProbeConfig()
        );
    }

    @Override
    public StateFuzzerServerConfig buildServerConfig() {
        return new StateFuzzerServerConfig(
                new LearnerConfig(),
                new EdhocSulServerConfig(new EdhocMapperConfig()),
                new TestRunnerConfig(),
                new TimingProbeConfig()
        );
    }

    @Override
    public StateFuzzer build(StateFuzzerEnabler stateFuzzerEnabler) {
        return new StateFuzzerStandard(
                new StateFuzzerComposerStandard(stateFuzzerEnabler, alphabetBuilder, sulBuilder, sulWrapper)
        );
    }

    @Override
    public TestRunner build(TestRunnerEnabler testRunnerEnabler) {
        return new TestRunner(testRunnerEnabler, alphabetBuilder, sulBuilder, sulWrapper);
    }

    @Override
    public TimingProbe build(TimingProbeEnabler timingProbeEnabler) {
        return new TimingProbe(timingProbeEnabler, alphabetBuilder, sulBuilder, sulWrapper);
    }
}
