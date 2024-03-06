package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import de.learnlib.ralib.data.DataType;
import jakarta.xml.bind.annotation.XmlAttribute;

public class InputSymbolXml {

    private String name = null;
    private DataType[] types = null;

    public InputSymbolXml() {
    }

    @XmlAttribute(name = "name", required = true)
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @XmlAttribute(name = "datatype", required = false)
    public void setDataTypes(DataType[] types) {
        this.types = types;
    }

    public DataType[] getDataTypes() {
        return this.types;
    }
}
