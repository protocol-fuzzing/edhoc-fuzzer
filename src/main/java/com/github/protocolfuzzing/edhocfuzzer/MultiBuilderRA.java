package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.EdhocSulBuilderRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulClientConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulServerConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
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
import org.eclipse.californium.edhoc.SharedSecretCalculation;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiBuilderRA implements
                StateFuzzerConfigBuilder,
                StateFuzzerBuilder<RegisterAutomatonWrapper<ParameterizedSymbol, PSymbolInstance>>,
                TestRunnerBuilder,
                TimingProbeBuilder {

        // protected AlphabetBuilderStandard<SymbolXml> alphabetBuilder = new
        // AlphabetBuilderStandard<>(
        // new AlphabetSerializerXml<SymbolXml, EdhocAlphabetPojoXmlRA>(SymbolXml.class,
        // EdhocAlphabetPojoXmlRA.class));

        // protected AlphabetTransformerRA alphabetTransformer = new
        // AlphabetTransformerRA(alphabetBuilder);

        DataType T_CI = new DataType("C_I", Integer.class);

        protected EnumAlphabet alphabet = new EnumAlphabet.Builder()
                        .withInputs(MessageInputTypeRA.values())
                        .withOutputs(MessageOutputTypeRA.values())
                        .withOutputs(PSFOutputSymbols.values())
                        .withInput(MessageInputTypeRA.EDHOC_MESSAGE_1_INPUT, T_CI)
                        .withInput(MessageInputTypeRA.EDHOC_MESSAGE_2_INPUT, T_CI)
                        .withInput(MessageInputTypeRA.EDHOC_MESSAGE_3_INPUT, T_CI)
                        .withInput(MessageInputTypeRA.EDHOC_MESSAGE_4_INPUT, T_CI)
                        .withInput(MessageInputTypeRA.EDHOC_MESSAGE_3_OSCORE_APP_INPUT, T_CI)
                        .withOutput(MessageOutputTypeRA.EDHOC_MESSAGE_1_OUTPUT, T_CI)
                        .withOutput(MessageOutputTypeRA.EDHOC_MESSAGE_2_OUTPUT, T_CI)
                        .withOutput(MessageOutputTypeRA.EDHOC_MESSAGE_3_OUTPUT, T_CI)
                        .withOutput(MessageOutputTypeRA.EDHOC_MESSAGE_4_OUTPUT, T_CI)
                        .withOutput(MessageOutputTypeRA.EDHOC_MESSAGE_3_OSCORE_APP_OUTPUT, T_CI)
                        .build();

        protected AlphabetDummyBuilder<ParameterizedSymbol> dummyBuilder = new AlphabetDummyBuilder<ParameterizedSymbol>(
                        alphabet);

        protected SulBuilder<PSymbolInstance, PSymbolInstance, EdhocExecutionContextRA> sulBuilder = new EdhocSulBuilderRA(
                        alphabet);
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
                // testOneKeyBuild();
                @SuppressWarnings("rawtypes")
                final Map<DataType, Theory> teachers = new LinkedHashMap<>();
                teachers.put(T_CI, new IntegerEqualityTheory(T_CI));
                return new StateFuzzerRA<>(
                                new StateFuzzerComposerRA<ParameterizedSymbol, EdhocExecutionContextRA>(
                                                stateFuzzerEnabler,
                                                dummyBuilder, sulBuilder, sulWrapper, teachers).initialize());
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

        public static void testOneKeyBuild() {
                byte[] invalidG_Y = new byte[] { (byte) 0x00, (byte) 0x33, (byte) 0x69, (byte) 0xf1, (byte) 0xa6,
                                (byte) 0x0d, (byte) 0x17, (byte) 0xe8, (byte) 0x51, (byte) 0x15,
                                (byte) 0x88, (byte) 0x67, (byte) 0xdd, (byte) 0x33, (byte) 0xeb, (byte) 0xad,
                                (byte) 0x87, (byte) 0x19, (byte) 0xeb, (byte) 0xb0,
                                (byte) 0xd5, (byte) 0xe9, (byte) 0x08, (byte) 0xa3, (byte) 0xeb, (byte) 0x6d,
                                (byte) 0x5f, (byte) 0x48, (byte) 0x12, (byte) 0xb5,
                                (byte) 0x85, (byte) 0xdf };

                byte[] validG_Y = new byte[] { (byte) 0xf7, (byte) 0x02, (byte) 0xe5, (byte) 0xee, (byte) 0x70,
                                (byte) 0x46, (byte) 0xb4, (byte) 0xea, (byte) 0xfe, (byte) 0x7a,
                                (byte) 0x31, (byte) 0x0d, (byte) 0x2f, (byte) 0xff, (byte) 0x04, (byte) 0xba,
                                (byte) 0xc6, (byte) 0xa5, (byte) 0x96, (byte) 0x9a,
                                (byte) 0x84, (byte) 0x39, (byte) 0xf5, (byte) 0x4a, (byte) 0xd5, (byte) 0xba,
                                (byte) 0x3a, (byte) 0x26, (byte) 0xcc, (byte) 0xe8,
                                (byte) 0x7b, (byte) 0xb0 };
                System.err.println("Key beggining with non-zero byte:");
                System.err.println("Curve25519");
                System.err.println(SharedSecretCalculation.buildCurve25519OneKey(null, validG_Y) == null ? "Key null"
                                : "Key not null");

                System.err.println("ECDSA256");
                System.err.println(
                                SharedSecretCalculation.buildEcdsa256OneKey(null, validG_Y, null) == null ? "Key null"
                                                : "Key not null");
                System.err.println("Key beginning with zero byte:");
                System.err.println("Curve25519");
                System.err.println(SharedSecretCalculation.buildCurve25519OneKey(null, invalidG_Y) == null ? "Key null"
                                : "Key not null");
                System.err.println("ECDSA256");
                System.err.println(
                                SharedSecretCalculation.buildEcdsa256OneKey(null, invalidG_Y, null) == null ? "Key null"
                                                : "Key not null");
                System.exit(0);
        }
}
