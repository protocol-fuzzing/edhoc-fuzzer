package gr.ntua.softlab.protocolStateFuzzer.learner.config;

import com.beust.jcommander.Parameter;
import gr.ntua.softlab.protocolStateFuzzer.learner.factory.EquivalenceAlgorithmName;
import gr.ntua.softlab.protocolStateFuzzer.learner.factory.LearningAlgorithmName;

import java.time.Duration;
import java.util.List;

public class LearningConfig {
    @Parameter(names = "-learningAlgorithm", description = "Which algorithm should be used for learning")
    protected LearningAlgorithmName learningAlgorithm = LearningAlgorithmName.TTT;

    @Parameter(names = "-equivalenceAlgorithms", description = "Which test algorithms should be used for equivalence testing")
    protected List<EquivalenceAlgorithmName> equivalenceAlgorithms = List.of(EquivalenceAlgorithmName.RANDOM_WP_METHOD);

    @Parameter(names = "-depth", description = "Maximal depth ( W/WP Method)")
    protected Integer maxDepth = 1;

    @Parameter(names = "-minLength", description = "Min length (random words, Random WP Method)")
    protected Integer minLength = 5;

    @Parameter(names = "-maxLength", description = "Max length (random words)")
    protected Integer maxLength = 15;

    @Parameter(names = "-randLength", description = "Size of the random part (Random WP Method)")
    protected Integer randLength = 5;

    @Parameter(names = "-queries", description = "Number of queries (all)")
    protected Integer numberOfQueries = 1000;

    @Parameter(names = "-memQueryRuns", description = "The number of times each membership query is executed before an answer is returned. Setting it to more than 1 enables an multiple-run oracle which may prevent non-determinism.")
    protected Integer runsPerMembershipQuery = 1;

    @Parameter(names = "-memQueryRetries", description = "The number of times a membership query is executed in case cache inconsistency is detected.")
    protected Integer membershipQueryRetries = 3;

    @Parameter(names = "-queryFile", description = "If set, logs all membership queries to this file.")
    protected String queryFile = null;

    @Parameter(names = "-probReset", description = "Probability of stopping execution of a test after each input")
    protected Integer probReset = 0;

    @Parameter(names = "-testFile", description = "A file with tests to be run.")
    protected String testFile = null;

    @Parameter(names = "-seed", description = "Seed used for random value generation.")
    protected Long seed = 0L;

    @Parameter(names = "-cacheTests", description = "Cache tests, which increases the memory footprint but improves performance. It also renders useless most forms of non-determinism sanitization")
    protected boolean cacheTests = false;

    @Parameter(names = "-dontCacheTests", description = "Deprecated parameter with no effect, kept for backwards compatibility. Use -cacheTests.")
    protected boolean dontCacheTests = false;

    @Parameter(names = "-ceSanitization", description = "Activates CE sanitization, which involves re-running potential CE's ensuring they are not spurious")
    protected boolean ceSanitization = true;

    @Parameter(names = "-skipNonDetTests", description = "Rather than throw an exception, logs and skips tests whose execution turned out non-deterministic")
    protected boolean skipNonDetTests = false;

    @Parameter(names = "-ceReruns", description = "Represents the number of times a CE is re-run in order for it to be confirmed")
    protected Integer ceReruns = 3;

    @Parameter(names = "-probabilisticSanitization", description = "Enables probabilistic sanitization of the CEs resulting in non determinism")
    protected boolean probabilisticSanitization = true;

    @Parameter(names = "-timeLimit", description = "If set, imposes a time limit on the learning experiment. Once this time ellapses, "
            + "learning is stopped and statistics for the incomplete learning run are published", converter = DurationConverter.class)
    protected Duration timeLimit = null;

    public LearningAlgorithmName getLearningAlgorithm() {
        return learningAlgorithm;
    }

    public List<EquivalenceAlgorithmName> getEquivalenceAlgorithms() {
        return equivalenceAlgorithms;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getNumberOfQueries() {
        return numberOfQueries;
    }

    public int getProbReset() {
        return probReset;
    }

    public int getRandLength() {
        return randLength;
    }

    public String getTestFile() {
        return testFile;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isCacheTests() {
        return cacheTests;
    }

    public int getCeReruns() {
        return ceReruns;
    }

    public String getQueryFile() {
        return queryFile;
    }

    public boolean isCeSanitization() {
        return ceSanitization;
    }

    public boolean isSkipNonDetTests() {
        return skipNonDetTests;
    }

    public boolean isProbabilisticSanitization() {
        return probabilisticSanitization;
    }

    public Duration getTimeLimit() {
        return timeLimit;
    }

    public int getRunsPerMembershipQuery() {
        return runsPerMembershipQuery;
    }

    public int getMembershipQueryRetries() {
        return membershipQueryRetries;
    }
}
