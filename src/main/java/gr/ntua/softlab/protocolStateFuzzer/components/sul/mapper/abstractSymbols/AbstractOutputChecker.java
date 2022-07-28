package gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols;

/**
 * Provides an interface for analyzing outputs so that how the actual strings are formed
 * is decoupled from the checking code.
 */
public interface AbstractOutputChecker {
	boolean hasInitialClientMessage(AbstractOutput abstractOutput);
}
