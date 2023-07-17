package gr.ntua.softlab.edhocfuzzer.components.learner;

import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.xml.AlphabetPojoXml;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractInput;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.symbols.inputs.*;
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
