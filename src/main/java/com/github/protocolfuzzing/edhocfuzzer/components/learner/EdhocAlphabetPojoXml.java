package com.github.protocolfuzzing.edhocfuzzer.components.learner;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.*;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.xml.AlphabetPojoXml;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "alphabet")
@XmlAccessorType(XmlAccessType.FIELD)
public class EdhocAlphabetPojoXml extends AlphabetPojoXml<EdhocInput> {
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
    protected List<EdhocInput> inputs;

    public EdhocAlphabetPojoXml() {}

    public EdhocAlphabetPojoXml(List<EdhocInput> inputs) {
        this.inputs = inputs;
    }

    @Override
    public List<EdhocInput> getInputs(){
        return inputs;
    }
}
