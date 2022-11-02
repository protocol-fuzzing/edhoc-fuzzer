package gr.ntua.softlab.edhocFuzzer.components.learner;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs.*;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.alphabet.xml.AlphabetPojoXml;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlRootElement(name = "alphabet")
@XmlAccessorType(XmlAccessType.FIELD)
public class EdhocAlphabetPojoXml extends AlphabetPojoXml {
    @XmlElements(value = {
            @XmlElement(type = EdhocMessage1Input.class, name = "EdhocMessage1Input"),
            @XmlElement(type = EdhocMessage2Input.class, name = "EdhocMessage2Input"),
            @XmlElement(type = EdhocMessage3Input.class, name = "EdhocMessage3Input"),
            @XmlElement(type = EdhocMessage4Input.class, name = "EdhocMessage4Input"),
            @XmlElement(type = EdhocErrorMessageInput.class, name = "EdhocErrorMessageInput"),
            @XmlElement(type = EdhocMessage3OscoreAppInput.class, name = "EdhocMessage3OscoreAppInput"),
            @XmlElement(type = OscoreAppMessageInput.class, name = "OscoreAppMessageInput"),
            @XmlElement(type = CoapAppMessageInput.class, name = "CoapAppMessageInput"),
            @XmlElement(type = CoapEmptyMessageInput.class, name = "CoapEmptyMessageInput")
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
