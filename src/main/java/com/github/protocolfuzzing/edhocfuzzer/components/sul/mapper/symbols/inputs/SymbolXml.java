package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Symbol")
public class SymbolXml {

    @XmlElement(name = "Name")
    private String name = null;

    @XmlElement(name = "Type")
    private Type symbolType = Type.INPUT;

    @XmlElement(name = "DataTypes")
    private DataTypeXml[] types = {};

    public SymbolXml(String name, Type type, DataTypeXml... types) {
        this.name = name;
        this.symbolType = type;
        this.types = types;
    }

    public SymbolXml() {
    }

    public String getName() {
        return this.name;
    }

    public DataTypeXml[] getDataTypes() {
        return this.types;
    }

    public Type getSymbolType() {
        return this.symbolType;
    }

    public enum Type {
        INPUT,
        OUTPUT
    }
}
