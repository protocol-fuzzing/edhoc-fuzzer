package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.MapperOutput;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.util.List;

public class EdhocOutputRA extends PSymbolInstance implements MapperOutput<EdhocOutputRA, EdhocProtocolMessage> {
    List<EdhocProtocolMessage> messages;

    public EdhocOutputRA(ParameterizedSymbol baseSymbol, @SuppressWarnings("rawtypes") DataValue... parameterValues) {
        super(baseSymbol, parameterValues);
    }

    public EdhocOutputRA(List<EdhocProtocolMessage> messages, ParameterizedSymbol baseSymbol, @SuppressWarnings("rawtypes") DataValue... parameterValues) {
        super(baseSymbol, parameterValues);
        this.messages = messages;
    }

    /** Used only by @link #EdhocOutputBuilderRA. */
    public EdhocOutputRA(String name) {
        super(new OutputSymbol(name, (DataType[]) null), (DataValue[]) null);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EdhocOutputRA) {
            return super.equals((PSymbolInstance) other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean isComposite() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAtomic() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<EdhocProtocolMessage> getMessages() {
        return this.messages;
    }

    @Override
    public String getName() {
        return super.getBaseSymbol().getName();
    }

    @Override
    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    @Override
    public List<EdhocOutputRA> getAtomicOutputs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicOutputs'");
    }

    @Override
    public List<EdhocOutputRA> getAtomicOutputs(int unrollRepeating) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicOutputs'");
    }

    @Override
    public List<String> getAtomicAbstractionStrings() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicAbstractionStrings'");
    }

    @Override
    public List<String> getAtomicAbstractionStrings(int unrollRepeating) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicAbstractionStrings'");
    }

    @Override
    public boolean isRepeating() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isRepeating'");
    }

    @Override
    public EdhocOutputRA getRepeatedOutput() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRepeatedOutput'");
    }

    @Override
    public String toDetailedString() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toDetailedString'");
    }
}
