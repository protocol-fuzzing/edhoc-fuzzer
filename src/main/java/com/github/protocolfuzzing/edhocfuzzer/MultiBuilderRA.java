package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.learner.EdhocAlphabetPojoXmlRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.EdhocSulBuilderRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulClientConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulServerConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.AlphabetBuilderStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.xml.AlphabetSerializerXml;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.config.LearnerConfigRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.statistics.RegisterAutomatonWrapper;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SulBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SulWrapper;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SulWrapperStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzer;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerComposerRA;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerRA;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerClientConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerClientConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerConfigBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerEnabler;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerServerConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerServerConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.TestRunner;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.TestRunnerBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.TestRunnerStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.config.TestRunnerConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.config.TestRunnerEnabler;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbe;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbeBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbeStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.config.TimingProbeConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.config.TimingProbeEnabler;
import com.upokecenter.cbor.CBORObject;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiBuilderRA implements
        StateFuzzerConfigBuilder,
        StateFuzzerBuilder<RegisterAutomatonWrapper<EdhocInputRA>>,
        TestRunnerBuilder,
        TimingProbeBuilder {

    protected AlphabetBuilder<EdhocInputRA> alphabetBuilder = new AlphabetBuilderStandard<>(
            new AlphabetSerializerXml<EdhocInputRA, EdhocAlphabetPojoXmlRA>(EdhocInputRA.class,
                    EdhocAlphabetPojoXmlRA.class));

    protected SulBuilder<EdhocInputRA, EdhocOutputRA, EdhocExecutionContextRA> sulBuilder = new EdhocSulBuilderRA();
    protected SulWrapper<EdhocInputRA, EdhocOutputRA, EdhocExecutionContextRA> sulWrapper = new SulWrapperStandard<>();

    @Override
    public StateFuzzerClientConfig buildClientConfig() {
        return new StateFuzzerClientConfigStandard(
                new LearnerConfigRA(),
                new EdhocSulClientConfig(new EdhocMapperConfig()),
                new TestRunnerConfigStandard(),
                new TimingProbeConfigStandard());
    }

    @Override
    public StateFuzzerServerConfig buildServerConfig() {
        return new StateFuzzerServerConfigStandard(
                new LearnerConfigRA(),
                new EdhocSulServerConfig(new EdhocMapperConfig()),
                new TestRunnerConfigStandard(),
                new TimingProbeConfigStandard());
    }

    @Override
    public StateFuzzer<RegisterAutomatonWrapper<EdhocInputRA>> build(StateFuzzerEnabler stateFuzzerEnabler) {
        DataType T_CI = new DataType("C_I", CBORObject.class);
        @SuppressWarnings("rawtypes")
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_CI, new IntegerEqualityTheory(T_CI));
        return new StateFuzzerRA<>(
                new StateFuzzerComposerRA<EdhocInputRA, EdhocOutputRA, EdhocExecutionContextRA>(stateFuzzerEnabler,
                        alphabetBuilder, sulBuilder, sulWrapper, teachers).initialize());
    }

    @Override
    public TestRunner build(TestRunnerEnabler testRunnerEnabler) {
        return new TestRunnerStandard<>(testRunnerEnabler, alphabetBuilder, sulBuilder, sulWrapper).initialize();
    }

    @Override
    public TimingProbe build(TimingProbeEnabler timingProbeEnabler) {
        return new TimingProbeStandard<>(timingProbeEnabler, alphabetBuilder, sulBuilder, sulWrapper).initialize();
    }
}
