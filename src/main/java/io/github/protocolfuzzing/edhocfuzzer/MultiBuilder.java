package io.github.protocolfuzzing.edhocfuzzer;

import io.github.protocolfuzzing.edhocfuzzer.components.learner.EdhocAlphabetPojoXml;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.EdhocSulBuilder;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulClientConfig;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulServerConfig;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import io.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.xml.AlphabetSerializerXml;
import io.github.protocolfuzzing.protocolstatefuzzer.components.learner.config.LearnerConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.components.learner.statistics.MealyMachineWrapper;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzer;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerComposerStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerClientConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerClientConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerConfigBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerEnabler;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerServerConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerServerConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.DiffTester;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.DiffTesterBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.DiffTesterEnabler;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.DiffTesterStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.config.DiffTesterConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.config.DiffTesterConfigBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.difftester.config.DiffTesterConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.Fingerprint;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.FingerprintBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.FingerprintStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.config.FingerprintConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.config.FingerprintConfigBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.config.FingerprintConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.fingerprint.core.config.FingerprintEnabler;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.identifier.core.Identifier;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.identifier.core.IdentifierBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.identifier.core.IdentifierStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.identifier.core.config.IdentifierConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.identifier.core.config.IdentifierEnabler;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.TestRunner;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.TestRunnerBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.TestRunnerStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.config.TestRunnerConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.config.TestRunnerEnabler;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbe;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbeBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbeStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.config.TimingProbeConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.config.TimingProbeEnabler;

public class MultiBuilder implements
    StateFuzzerConfigBuilder,
    DiffTesterConfigBuilder,
    FingerprintConfigBuilder,
    StateFuzzerBuilder<MealyMachineWrapper<EdhocInput, EdhocOutput>>,
    DiffTesterBuilder,
    FingerprintBuilder,
    TestRunnerBuilder,
    TimingProbeBuilder,
    IdentifierBuilder<MealyMachineWrapper<EdhocInput, EdhocOutput>> {

    protected AlphabetBuilder<EdhocInput> alphabetBuilder = new AlphabetBuilderStandard<>(
        new AlphabetSerializerXml<EdhocInput, EdhocAlphabetPojoXml>(EdhocInput.class, EdhocAlphabetPojoXml.class)
    );

    protected SULBuilder<EdhocInput, EdhocOutput, EdhocExecutionContext> sulBuilder = new EdhocSulBuilder();

    @Override
    public StateFuzzerClientConfig buildClientConfig() {
        return new StateFuzzerClientConfigStandard(
                new LearnerConfigStandard(),
                new EdhocSulClientConfig(new EdhocMapperConfig()),
                new TestRunnerConfigStandard(),
                new TimingProbeConfigStandard(),
                null
        );
    }

    @Override
    public StateFuzzerServerConfig buildServerConfig() {
        return new StateFuzzerServerConfigStandard(
                new LearnerConfigStandard(),
                new EdhocSulServerConfig(new EdhocMapperConfig()),
                new TestRunnerConfigStandard(),
                new TimingProbeConfigStandard(),
                new IdentifierConfigStandard()
        );
    }

    @Override
    public DiffTesterConfig buildConfig() {
        return new DiffTesterConfigStandard();
    }

    @Override
    public FingerprintConfig buildFingerprintConfig() {
        return new FingerprintConfigStandard();
    }

    @Override
    public StateFuzzer<MealyMachineWrapper<EdhocInput, EdhocOutput>> build(StateFuzzerEnabler stateFuzzerEnabler) {
        return new StateFuzzerStandard<>(
            new StateFuzzerComposerStandard<>(stateFuzzerEnabler, alphabetBuilder, sulBuilder).initialize()
        );
    }

    @Override
    public DiffTester build(DiffTesterEnabler diffTesterEnabler) {
        return new DiffTesterStandard<>(diffTesterEnabler, alphabetBuilder);
    }

    @Override
    public Fingerprint build(FingerprintEnabler fingerprintEnabler) {
        return new FingerprintStandard<>(fingerprintEnabler, alphabetBuilder);
    }

    @Override
    public TestRunner build(TestRunnerEnabler testRunnerEnabler) {
        return new TestRunnerStandard<>(testRunnerEnabler, alphabetBuilder, sulBuilder).initialize();
    }

    @Override
    public TimingProbe build(TimingProbeEnabler timingProbeEnabler) {
        return new TimingProbeStandard<>(timingProbeEnabler, alphabetBuilder, sulBuilder).initialize();
    }

    @Override
    public Identifier<MealyMachineWrapper<EdhocInput, EdhocOutput>> build(IdentifierEnabler identifierEnabler) {
        return new IdentifierStandard<>(identifierEnabler, alphabetBuilder, sulBuilder).initialize();
    }
}
