package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.MapperInput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;
import com.upokecenter.cbor.CBORObject;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;


public abstract class EdhocInputRA extends PSymbolInstance
        implements MapperInput<EdhocOutputRA, EdhocProtocolMessage, EdhocExecutionContextRA> {

    private long extendedWait = 0;
    protected DataType T_CI = new DataType("C_I", CBORObject.class);


    EdhocInputRA(ParameterizedSymbol baseSymbol, DataValue<?>... parameterValues) {
        super(baseSymbol, parameterValues);
    }

    public abstract Enum<MessageInputType> getInputType();

    @Override
    public void preSendUpdate(EdhocExecutionContextRA context) {
    }

    @Override
    public void postSendUpdate(EdhocExecutionContextRA context) {
    }

    @Override
    public void postReceiveUpdate(
            EdhocOutputRA output,
            OutputChecker<EdhocOutputRA> abstractOutputChecker,
            EdhocExecutionContextRA context) {
    }

    @Override
    public Long getExtendedWait() {
        return extendedWait;
    }

    @Override
    public void setExtendedWait(Long value) {
        extendedWait = value;
    }

    @Override
    public String getName() {
        return this.getBaseSymbol().getName();
    }

    public DataType[] getDataTypes() {
        return this.getBaseSymbol().getPtypes();
    }

    /*
     * TODO This is bad in multiple ways:
     * - We need to have access to the datatype, which means defining it multiple
     * times. For teachers, EdhocInputRA and the EdhocOutputMapperRA.
     * - It is uncertain if we can cast back to CBORObject, since the learner
     * selects a random new value, which means it is unreasonable to assume it can
     * convert to any type, since it only has the integer equality theories.
     * - If the C_I is a bytestring it is unclear if use of a mapper to convert from
     * a randomly selected integer in the learner to a corresponding bytestring is
     * possible.
     */
    public void updatePeerConnectionId(EdhocSessionPersistent session) {
        for (DataValue<?> dv : this.getParameterValues()) {
            if (dv.getType().equals(T_CI)) {

                // TODO: Unsafe typecast.
                CBORObject value = (CBORObject) dv.getId();
                session.setPeerConnectionId(value.EncodeToBytes());
            }
        }
    }
}
