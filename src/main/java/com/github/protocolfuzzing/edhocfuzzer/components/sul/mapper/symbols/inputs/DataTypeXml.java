package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class DataTypeXml {

    private String name = null;

    private Class<?> base = null;

    public DataTypeXml(String name, Class<?> base) {
        this.name = name;
        this.base = base;
    }

    public DataTypeXml() {
        this(null, null);
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getBase() {
        return this.base;
    }
}
