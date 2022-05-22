package gr.ntua.softlab.edhocFuzzer.learner;

import gr.ntua.softlab.edhocFuzzer.mapper.symbols.*;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.xml.AlphabetPojoXml;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "alphabet")
@XmlAccessorType(XmlAccessType.FIELD)
public class EdhocAlphabetPojoXml extends AlphabetPojoXml {
    @XmlElements(value = {
            @XmlElement(type = Message1Input.class, name = "Message1Input"),
            @XmlElement(type = Message2Input.class, name = "Message2Input"),
            @XmlElement(type = Message3Input.class, name = "Message3Input"),
            @XmlElement(type = Message4Input.class, name = "Message4Input"),
            @XmlElement(type = ErrorMessageInput.class, name = "ErrorMessageInput"),
            @XmlElement(type = ApplicationDataInput.class, name = "ApplicationDataInput")
    })
    protected List<AbstractInput> inputs;

    public EdhocAlphabetPojoXml() {}

    public EdhocAlphabetPojoXml(List<AbstractInput> inputs) {
        this.inputs = inputs;
    }

    public List<AbstractInput> getInputs(){
        return inputs;
    }
}
