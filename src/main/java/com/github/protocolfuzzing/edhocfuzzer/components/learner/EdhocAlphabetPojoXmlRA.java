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
            @XmlElement(type = InputSymbolXml.class, name = "EdhocMessage1Input"),
            @XmlElement(type = InputSymbolXml.class, name = "EdhocMessage2Input"),
            @XmlElement(type = InputSymbolXml.class, name = "EdhocMessage3Input"),
            @XmlElement(type = InputSymbolXml.class, name = "EdhocMessage4Input"),
            @XmlElement(type = InputSymbolXml.class, name = "EdhocErrorMessageInput"),
            @XmlElement(type = InputSymbolXml.class, name = "EdhocMessage3OscoreAppInput"),
            @XmlElement(type = InputSymbolXml.class, name = "OscoreAppMessageInput"),
            @XmlElement(type = InputSymbolXml.class, name = "CoapAppMessageInput"),
            @XmlElement(type = InputSymbolXml.class, name = "CoapEmptyMessageInput")
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
