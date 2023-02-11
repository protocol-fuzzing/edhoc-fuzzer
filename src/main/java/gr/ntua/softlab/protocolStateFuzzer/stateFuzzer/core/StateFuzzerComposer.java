package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core;

import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.statistics.StatisticsTracker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import java.io.File;
import java.io.InputStream;

public interface StateFuzzerComposer {

    String NON_DET_FILENAME = "nondet.log";
    String QUERY_FILENAME = "query.log";

    StatisticsTracker getStatisticsTracker();

    LearningAlgorithm.MealyLearner<AbstractInput, AbstractOutput> getLearner();

    EquivalenceOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput, Word<AbstractOutput>>
    getEquivalenceOracle();

    Alphabet<AbstractInput> getAlphabet();

    InputStream getAlphabetFileInputStream();

    String getAlphabetFileExtension();

    StateFuzzerEnabler getStateFuzzerEnabler();

    File getOutputDir();

    CleanupTasks getCleanupTasks();
}
