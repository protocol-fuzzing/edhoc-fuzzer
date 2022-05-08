package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer;

import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerConfig;
import gr.ntua.softlab.protocolStateFuzzer.learner.statistics.StatisticsTracker;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import java.io.File;

public interface StateFuzzerComposer {

    String NON_DET_FILENAME = "nondet.log";

    StatisticsTracker getStatisticsTracker();

    LearningAlgorithm.MealyLearner<AbstractInput, AbstractOutput> getLearner();

    EquivalenceOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput, Word<AbstractOutput>>
    getEquivalenceOracle();

    Alphabet<AbstractInput> getAlphabet();

    StateFuzzerConfig getStateFuzzerConfig();

    File getOutputFolder();

    CleanupTasks getCleanupTasks();
}
