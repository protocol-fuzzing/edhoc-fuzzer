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
public class EdhocAlphabetPojoXmlRA extends AlphabetPojoXml<InputSymbolXml> {
    @XmlElements(value = {
            @XmlElement(type = EdhocMessage1InputRA.class, name = "EdhocMessage1Input"),
            @XmlElement(type = EdhocMessage2InputRA.class, name = "EdhocMessage2Input"),
            @XmlElement(type = EdhocMessage3InputRA.class, name = "EdhocMessage3Input"),
            @XmlElement(type = EdhocMessage4InputRA.class, name = "EdhocMessage4Input"),
            @XmlElement(type = EdhocErrorMessageInputRA.class, name = "EdhocErrorMessageInput"),
            @XmlElement(type = EdhocMessage3OscoreAppInputRA.class, name = "EdhocMessage3OscoreAppInput"),
            @XmlElement(type = OscoreAppMessageInputRA.class, name = "OscoreAppMessageInput"),
            @XmlElement(type = CoapAppMessageInputRA.class, name = "CoapAppMessageInput"),
            @XmlElement(type = CoapEmptyMessageInputRA.class, name = "CoapEmptyMessageInput")
    })
    protected List<InputSymbolXml> inputs;

    public EdhocAlphabetPojoXmlRA() {}

    public EdhocAlphabetPojoXmlRA(List<InputSymbolXml> inputs) {
        this.inputs = inputs;
    }

    @Override
    public List<InputSymbolXml> getInputs(){
        return inputs;
    }
}
