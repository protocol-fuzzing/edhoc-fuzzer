package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer;

import de.learnlib.api.SUL;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.oracle.membership.SULOracle;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerConfig;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearningConfig;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.learner.factory.LearnerFactory;
import gr.ntua.softlab.protocolStateFuzzer.learner.oracles.*;
import gr.ntua.softlab.protocolStateFuzzer.learner.statistics.StatisticsTracker;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.sul.WrappedSulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StateFuzzerComposerStandard implements StateFuzzerComposer {
    protected final StateFuzzerConfig stateFuzzerConfig;
    protected final LearningConfig learningConfig;
    protected final Alphabet<AbstractInput> alphabet;
    protected final SUL<AbstractInput, AbstractOutput> sul;
    protected final ObservationTree<AbstractInput, AbstractOutput> cache;
    protected final File outputFolder;
    protected final FileWriter nonDetWriter;
    protected final CleanupTasks cleanupTasks;
    protected StatisticsTracker statisticsTracker;
    protected LearningAlgorithm.MealyLearner<AbstractInput, AbstractOutput> learner;
    protected EquivalenceOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput, Word<AbstractOutput>>
        equivalenceOracle;


    public StateFuzzerComposerStandard(
            StateFuzzerConfig stateFuzzerConfig, AlphabetBuilder alphabetBuilder,
            MapperBuilder mapperBuilder, WrappedSulBuilder wrappedSulBuilder){
        this.stateFuzzerConfig = stateFuzzerConfig;
        this.learningConfig = stateFuzzerConfig.getLearningConfig();

        // de-serialize and build alphabet
        this.alphabet = alphabetBuilder.build(stateFuzzerConfig);

        // set up output directory
        this.outputFolder = new File(stateFuzzerConfig.getOutput());
        this.outputFolder.mkdirs();

        // initialize cleanup tasks
        this.cleanupTasks = new CleanupTasks();

        // set up SUL (System Under Learning)
        Mapper mapper = mapperBuilder.build(stateFuzzerConfig.getMapperConfig());

        this.sul = wrappedSulBuilder.build(stateFuzzerConfig.getSulConfig(), mapper, cleanupTasks);
        if (learningConfig.getTimeLimit() != null) {
            wrappedSulBuilder.setTimeLimit(this.sul, learningConfig.getTimeLimit());
        }

        // TODO the LOGGER instances should handle this, instead of us passing non det writers as arguments.
        try {
            this.nonDetWriter = new FileWriter(new File(outputFolder, NON_DET_FILENAME));
        } catch (IOException e) {
            throw new RuntimeException("Could not create non-determinism file writer");
        }

        AbstractOutput[] cacheTerminatingOutputs = null;
        if (stateFuzzerConfig.getMapperConfig().isSocketClosedAsTimeout()) {
            cacheTerminatingOutputs = new AbstractOutput[]{AbstractOutput.socketClosed()};
        }

        // initialize cache as observation tree
        this.cache = new ObservationTree<>();

        // compose statistics tracker, learner and equivalence oracle in specific order
        composeStatisticsTracker(wrappedSulBuilder.getInputCounter(), wrappedSulBuilder.getResetCounter());
        composeLearner(cacheTerminatingOutputs);
        composeEquivalenceOracle(cacheTerminatingOutputs);
    }

    @Override
    public StatisticsTracker getStatisticsTracker() {
        return statisticsTracker;
    }

    @Override
    public LearningAlgorithm.MealyLearner<AbstractInput, AbstractOutput> getLearner() {
        return learner;
    }

    @Override
    public EquivalenceOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput, Word<AbstractOutput>>
    getEquivalenceOracle() {
        return equivalenceOracle;
    }

    @Override
    public Alphabet<AbstractInput> getAlphabet(){
        return alphabet;
    }

    @Override
    public StateFuzzerConfig getStateFuzzerConfig() {
        return stateFuzzerConfig;
    }

    @Override
    public File getOutputFolder() {
        return outputFolder;
    }

    @Override
    public CleanupTasks getCleanupTasks() {
        return cleanupTasks;
    }

    protected void composeStatisticsTracker(Counter inputCounter, Counter resetCounter) {
        this.statisticsTracker = new StatisticsTracker(inputCounter, resetCounter);
    }
    protected void composeLearner(AbstractOutput[] terminatingOutputs) {

        MembershipOracle.MealyMembershipOracle<AbstractInput, AbstractOutput> learningSulOracle = new SULOracle<>(sul);

        if (learningConfig.getRunsPerMembershipQuery() > 1) {
            learningSulOracle = new MultipleRunsSULOracle<>(learningConfig.getRunsPerMembershipQuery(),
                    learningSulOracle,true, nonDetWriter);
        }

        // a SUL oracle which uses the cache to check for non-determinism
        // and re-runs queries if non-det is detected
        learningSulOracle = new NonDeterminismRetryingSULOracle<>(learningSulOracle, cache,
                learningConfig.getMembershipQueryRetries(), true, nonDetWriter);

        // we are adding a cache so that executions of same inputs aren't repeated
        if (terminatingOutputs == null || terminatingOutputs.length == 0) {
            learningSulOracle = new CachingSULOracle<>(learningSulOracle, cache, false);
        } else {
            learningSulOracle = new CachingSULOracle<>(learningSulOracle, cache, false, terminatingOutputs);
        }

        if (learningConfig.getQueryFile() != null) {
            FileWriter queryWriter;
            try {
                queryWriter = new FileWriter(new File(outputFolder, learningConfig.getQueryFile()));
            } catch (IOException e1) {
                throw new RuntimeException("Could not create queryfile writer");
            }
            learningSulOracle = new LoggingSULOracle<>(learningSulOracle, queryWriter);
        }

        this.learner = LearnerFactory.loadLearner(learningConfig, learningSulOracle, alphabet);
    }

    protected void composeEquivalenceOracle(AbstractOutput[] terminatingOutputs) {

        MembershipOracle.MealyMembershipOracle<AbstractInput, AbstractOutput> testOracle = new SULOracle<>(sul);

        // in case sanitization is enabled, we apply a CE verification wrapper
        // to check counterexamples before they are returned to the EQ oracle
        if (learningConfig.isCeSanitization()) {
            testOracle = new CESanitizingSULOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput,
                    AbstractOutput>(
                        learningConfig.getCeReruns(), testOracle, learner::getHypothesisModel,
                        cache, learningConfig.isProbabilisticSanitization(), learningConfig.isSkipNonDetTests(),
                        nonDetWriter);
        }

        if (terminatingOutputs == null || terminatingOutputs.length == 0) {
            testOracle = new CachingSULOracle<>(testOracle, cache, !learningConfig.isCacheTests());
        } else {
            testOracle = new CachingSULOracle<>(testOracle, cache, !learningConfig.isCacheTests(), AbstractOutput.socketClosed());
        }

        this.equivalenceOracle = LearnerFactory.loadTester(learningConfig, sul, testOracle, alphabet);
    }
}
