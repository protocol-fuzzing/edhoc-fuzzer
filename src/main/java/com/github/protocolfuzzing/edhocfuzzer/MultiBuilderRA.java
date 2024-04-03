package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.learner.EdhocAlphabetPojoXmlRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.EdhocSulBuilderRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulClientConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulServerConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.SymbolXml;
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
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.config.TestRunnerConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.core.config.TestRunnerEnabler;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbe;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.TimingProbeBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.config.TimingProbeConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.testrunner.timingprobe.config.TimingProbeEnabler;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiBuilderRA implements
        StateFuzzerConfigBuilder,
        StateFuzzerBuilder<RegisterAutomatonWrapper<ParameterizedSymbol, PSymbolInstance>>,
        TestRunnerBuilder,
        TimingProbeBuilder {

    protected AlphabetBuilderStandard<SymbolXml> alphabetBuilder = new AlphabetBuilderStandard<>(
            new AlphabetSerializerXml<SymbolXml, EdhocAlphabetPojoXmlRA>(SymbolXml.class,
                    EdhocAlphabetPojoXmlRA.class));

    protected AlphabetTransformerRA alphabetTransformer = new AlphabetTransformerRA(alphabetBuilder);

    protected SulBuilder<PSymbolInstance, PSymbolInstance, EdhocExecutionContextRA> sulBuilder = new EdhocSulBuilderRA();
    protected SulWrapper<PSymbolInstance, PSymbolInstance, EdhocExecutionContextRA> sulWrapper = new SulWrapperStandard<>();

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
    public StateFuzzer<RegisterAutomatonWrapper<ParameterizedSymbol, PSymbolInstance>> build(
            StateFuzzerEnabler stateFuzzerEnabler) {
        DataType T_CI = new DataType("C_I", Integer.class);
        @SuppressWarnings("rawtypes")
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_CI, new IntegerEqualityTheory(T_CI));
        return new StateFuzzerRA<>(
                new StateFuzzerComposerRA<ParameterizedSymbol, EdhocExecutionContextRA>(stateFuzzerEnabler,
                        alphabetTransformer, sulBuilder, sulWrapper, teachers).initialize());
    }

    @Override
    public TestRunner build(TestRunnerEnabler testRunnerEnabler) {
        // return new TestRunnerStandard<>(testRunnerEnabler, alphabetTransformer,
        // sulBuilder, sulWrapper).initialize();
        return null; // FIXME: If this is used we have problems.
    }

    @Override
    public TimingProbe build(TimingProbeEnabler timingProbeEnabler) {
        // return new TimingProbeStandard<>(timingProbeEnabler, alphabetTransformer,
        // sulBuilder, sulWrapper).initialize();
        return null; // FIXME: If this is used we have problems.
    }
}
