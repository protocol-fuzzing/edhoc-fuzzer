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
public class EdhocAlphabetPojoXmlRA extends AlphabetPojoXml<SymbolXml> {
    @XmlElements(value = {
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage1Input"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage2Input"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage3Input"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage4Input"),
            @XmlElement(type = SymbolXml.class, name = "EdhocErrorMessageInput"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage3OscoreAppInput"),
            @XmlElement(type = SymbolXml.class, name = "OscoreAppMessageInput"),
            @XmlElement(type = SymbolXml.class, name = "CoapAppMessageInput"),
            @XmlElement(type = SymbolXml.class, name = "CoapEmptyMessageInput"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage1"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage2"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage3"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage4"),
            @XmlElement(type = SymbolXml.class, name = "EdhocErrorMessage"),
            @XmlElement(type = SymbolXml.class, name = "EdhocMessage3OscoreApp"),
            @XmlElement(type = SymbolXml.class, name = "OscoreAppMessage"),
            @XmlElement(type = SymbolXml.class, name = "CoapAppMessage"),
            @XmlElement(type = SymbolXml.class, name = "CoapEmptyMessage"),
            @XmlElement(type = SymbolXml.class, name = "Disabled"),
            @XmlElement(type = SymbolXml.class, name = "SocketClosed"),
            @XmlElement(type = SymbolXml.class, name = "Unknown"),
            @XmlElement(type = SymbolXml.class, name = "Timeout"),
            @XmlElement(type = SymbolXml.class, name = "CoapErrorMessage"),
            @XmlElement(type = SymbolXml.class, name = "CoapEmptyMessage"),
            @XmlElement(type = SymbolXml.class, name = "UnsupportedMessage"),
            @XmlElement(type = SymbolXml.class, name = "EdhocErrorMessage"),
            @XmlElement(type = SymbolXml.class, name = "CoapMessage"),
            @XmlElement(type = SymbolXml.class, name = "CoapAppMessage"),
            @XmlElement(type = SymbolXml.class, name = "OscoreAppMessage"),
    })
    protected List<SymbolXml> inputs;

    public EdhocAlphabetPojoXmlRA() {
    }

    public EdhocAlphabetPojoXmlRA(List<SymbolXml> inputs) {
        this.inputs = inputs;
    }

    @Override
    public List<SymbolXml> getInputs() {
        return inputs;
    }
}
