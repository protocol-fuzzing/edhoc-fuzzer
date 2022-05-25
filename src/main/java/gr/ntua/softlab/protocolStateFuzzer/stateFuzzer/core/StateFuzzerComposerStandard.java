package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core;

import de.learnlib.api.SUL;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.oracle.membership.SULOracle;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.factory.LearnerFactory;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.oracles.*;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.statistics.StatisticsTracker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.WrappedSulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StateFuzzerComposerStandard implements StateFuzzerComposer {
    protected final StateFuzzerEnabler stateFuzzerEnabler;
    protected final LearnerConfig learnerConfig;
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
            StateFuzzerEnabler stateFuzzerEnabler, AlphabetBuilder alphabetBuilder,
            MapperBuilder mapperBuilder, WrappedSulBuilder wrappedSulBuilder){
        this.stateFuzzerEnabler = stateFuzzerEnabler;
        this.learnerConfig = stateFuzzerEnabler.getLearnerConfig();

        // de-serialize and build alphabet
        this.alphabet = alphabetBuilder.build(stateFuzzerEnabler.getLearnerConfig());

        // set up output directory
        this.outputFolder = new File(stateFuzzerEnabler.getOutput());
        this.outputFolder.mkdirs();

        // initialize cleanup tasks
        this.cleanupTasks = new CleanupTasks();

        // set up SUL (System Under Learning)
        Mapper mapper = mapperBuilder.build(stateFuzzerEnabler.getMapperConfig());

        this.sul = wrappedSulBuilder.build(stateFuzzerEnabler.getSulConfig(), mapper, cleanupTasks);
        if (learnerConfig.getTimeLimit() != null) {
            wrappedSulBuilder.setTimeLimit(this.sul, learnerConfig.getTimeLimit());
        }

        // TODO the LOGGER instances should handle this, instead of us passing non det writers as arguments.
        try {
            this.nonDetWriter = new FileWriter(new File(outputFolder, NON_DET_FILENAME));
        } catch (IOException e) {
            throw new RuntimeException("Could not create non-determinism file writer");
        }

        AbstractOutput[] cacheTerminatingOutputs = null;
        if (stateFuzzerEnabler.getMapperConfig().isSocketClosedAsTimeout()) {
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
    public StateFuzzerEnabler getStateFuzzerEnabler() {
        return stateFuzzerEnabler;
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

        if (learnerConfig.getRunsPerMembershipQuery() > 1) {
            learningSulOracle = new MultipleRunsSULOracle<>(learnerConfig.getRunsPerMembershipQuery(),
                    learningSulOracle,true, nonDetWriter);
        }

        // a SUL oracle which uses the cache to check for non-determinism
        // and re-runs queries if non-det is detected
        learningSulOracle = new NonDeterminismRetryingSULOracle<>(learningSulOracle, cache,
                learnerConfig.getMembershipQueryRetries(), true, nonDetWriter);

        // we are adding a cache so that executions of same inputs aren't repeated
        if (terminatingOutputs == null || terminatingOutputs.length == 0) {
            learningSulOracle = new CachingSULOracle<>(learningSulOracle, cache, false);
        } else {
            learningSulOracle = new CachingSULOracle<>(learningSulOracle, cache, false, terminatingOutputs);
        }

        if (learnerConfig.getQueryFile() != null) {
            FileWriter queryWriter;
            try {
                queryWriter = new FileWriter(new File(outputFolder, learnerConfig.getQueryFile()));
            } catch (IOException e1) {
                throw new RuntimeException("Could not create queryfile writer");
            }
            learningSulOracle = new LoggingSULOracle<>(learningSulOracle, queryWriter);
        }

        this.learner = LearnerFactory.loadLearner(learnerConfig, learningSulOracle, alphabet);
    }

    protected void composeEquivalenceOracle(AbstractOutput[] terminatingOutputs) {

        MembershipOracle.MealyMembershipOracle<AbstractInput, AbstractOutput> testOracle = new SULOracle<>(sul);

        // in case sanitization is enabled, we apply a CE verification wrapper
        // to check counterexamples before they are returned to the EQ oracle
        if (learnerConfig.isCeSanitization()) {
            testOracle = new CESanitizingSULOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput,
                                            AbstractOutput>(
                        learnerConfig.getCeReruns(), testOracle, learner::getHypothesisModel,
                        cache, learnerConfig.isProbabilisticSanitization(), learnerConfig.isSkipNonDetTests(),
                        nonDetWriter);
        }

        if (terminatingOutputs == null || terminatingOutputs.length == 0) {
            testOracle = new CachingSULOracle<>(testOracle, cache, !learnerConfig.isCacheTests());
        } else {
            testOracle = new CachingSULOracle<>(testOracle, cache, !learnerConfig.isCacheTests(), AbstractOutput.socketClosed());
        }

        this.equivalenceOracle = LearnerFactory.loadTester(learnerConfig, sul, testOracle, alphabet);
    }
}
