package gr.ntua.softlab.protocolStateFuzzer.testRunner;

import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.oracle.membership.SULOracle;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.sul.WrappedSulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class TestRunner {
    private static final Logger LOGGER = LogManager.getLogger(TestRunner.class);
    protected TestRunnerEnabler testRunnerEnabler;
    protected Alphabet<AbstractInput> alphabet;
    protected Mapper mapper;
    protected MealyMembershipOracle<AbstractInput, AbstractOutput> sulOracle;
    protected MealyMachine<?, AbstractInput, ?, AbstractOutput> testSpecification;
    protected CleanupTasks cleanupTasks;

    public static <I, O> TestRunnerResult<I, O> runTest(Word<I> test, int times, MealyMembershipOracle<I, O> sulOracle) {
        LinkedHashMap<Word<O>, Integer> answerMap = new LinkedHashMap<>();
        for (int i = 0; i < times; i++) {
            Word<O> answer = sulOracle.answerQuery(test);
            if (!answerMap.containsKey(answer)) {
                answerMap.put(answer, 1);
            } else {
                answerMap.put(answer, answerMap.get(answer) + 1);
            }
        }
        return new TestRunnerResult<>(test, answerMap);
    }

    public TestRunner(TestRunnerEnabler testRunnerEnabler, AlphabetBuilder alphabetBuilder,
                      MapperBuilder mapperBuilder, WrappedSulBuilder wrappedSulBuilder) {
        this.testRunnerEnabler = testRunnerEnabler;
        this.alphabet = alphabetBuilder.build(testRunnerEnabler.getLearnerConfig());
        this.cleanupTasks = new CleanupTasks();

        this.mapper = mapperBuilder.build(testRunnerEnabler.getMapperConfig());

        this.sulOracle = new SULOracle<>(
                wrappedSulBuilder.build(testRunnerEnabler.getSulConfig(), mapper, cleanupTasks));

        this.testSpecification = null;
        if (testRunnerEnabler.getTestRunnerConfig().getTestSpecification() != null) {
            try {
                this.testSpecification = ModelFactory.buildProtocolModel(
                        alphabet, testRunnerEnabler.getTestRunnerConfig().getTestSpecification());
            } catch (IOException e) {
                throw new RuntimeException("Could not build protocol model from test specification: " + e.getMessage());
            }
        }

    }

    public Alphabet<AbstractInput> getAlphabet() {
        return alphabet;
    }

    public SulConfig getSulConfig() {
        return testRunnerEnabler.getSulConfig();
    }

    /**
     * Executes the tests in the config file and cleans up left-over processes once it is done.
     */
    public void run() throws IOException {
        try {
            List<TestRunnerResult<AbstractInput, AbstractOutput>> results = runTests();

            for (TestRunnerResult<AbstractInput, AbstractOutput> result : results) {
                LOGGER.info(result.toString());
                if (testRunnerEnabler.getTestRunnerConfig().isShowTransitionSequence()) {
                    LOGGER.info("");
                    LOGGER.info("Displaying Transition Sequence");
                    LOGGER.info("");
                    LOGGER.info(getTransitionSequenceString(result, !getSulConfig().isClient()));
                }
            }

        } finally {
            terminate();
        }
    }

    /**
     * Cleans up any left-over SUL process. Should be called only after all the desired tests have been executed.
     */
    public void terminate() {
        cleanupTasks.execute();
    }

    protected List<TestRunnerResult<AbstractInput, AbstractOutput>> runTests() throws IOException {
        TestParser testParser = new TestParser();
        List<Word<AbstractInput>> tests;
        String testFileOrTestString = testRunnerEnabler.getTestRunnerConfig().getTest();

        if (new File(testFileOrTestString).exists()) {
            tests = testParser.readTests(alphabet, testFileOrTestString);
        } else {
            LOGGER.info("File {} does not exist, interpreting argument as test", testFileOrTestString);
            String[] testStrings = testFileOrTestString.split("\\s+");
            tests = List.of(testParser.readTest(alphabet, Arrays.asList(testStrings)));
        }

        List<TestRunnerResult<AbstractInput, AbstractOutput>> results = new LinkedList<>();
        for (Word<AbstractInput> test : tests) {
            TestRunnerResult<AbstractInput, AbstractOutput> result = runTest(test);
            results.add(result);
        }
        return results;
    }

    protected TestRunnerResult<AbstractInput, AbstractOutput> runTest(Word<AbstractInput> test) {
        TestRunnerResult<AbstractInput, AbstractOutput> result = TestRunner.runTest(test,
                testRunnerEnabler.getTestRunnerConfig().getTimes(), sulOracle);

        if (testSpecification != null) {
            Word<AbstractOutput> outputWord = testSpecification.computeOutput(test);
            result.setExpectedOutputWord(outputWord);
        }
        return result;
    }

    protected String getTransitionSequenceString(TestRunnerResult<AbstractInput, AbstractOutput> result, boolean client) {
        StringBuilder sb = new StringBuilder();
        for (Word<AbstractOutput> answer : result.getGeneratedOutputs().keySet()) {
            sb.append(System.lineSeparator());
            for (int i = 0; i < result.getInputWord().size(); i++) {
                List<AbstractOutput> atomicOutputs = new LinkedList<>(answer.getSymbol(i).getAtomicOutputs(2));

                if (client && i == 0
                        && mapper.getAbstractOutputChecker().hasInitialClientMessage(atomicOutputs.get(0))) {
                    sb.append("- / ").append(atomicOutputs.get(0)).append(System.lineSeparator());
                    atomicOutputs.remove(0);
                }
                sb.append(result.getInputWord().getSymbol(i)).append(" / ");
                if (answer.getSymbol(i).isTimeout() || atomicOutputs.isEmpty()) {
                    sb.append("-");
                } else {
                    atomicOutputs.forEach(ao -> sb.append(ao).append("; "));
                    sb.deleteCharAt(sb.length() - 2);
                }
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
