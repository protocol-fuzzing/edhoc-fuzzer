package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "InputSymbol")
public class InputSymbolXml {

    @XmlElement(name="Name")
    private String name = null;

    @XmlElement(name="DataTypes")
    private DataTypeXml[] types = {};

    public InputSymbolXml(String name, DataTypeXml... types) {
        this.name = name;
        this.types = types;
    }

    public InputSymbolXml() {
    }

    public String getName() {
        return this.name;
    }

    public DataTypeXml[] getDataTypes() {
        return this.types;
    }
}
