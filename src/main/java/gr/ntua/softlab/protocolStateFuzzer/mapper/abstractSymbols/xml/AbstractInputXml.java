package gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.xml;

import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import jakarta.xml.bind.annotation.XmlAttribute;

public abstract class AbstractInputXml extends AbstractInput {
    public AbstractInputXml() {
        super();
    }

    public AbstractInputXml(String name) {
        super(name);
    }

    @XmlAttribute(name = "name", required = true)
    protected String name = null;

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @XmlAttribute(name = "extendedWait")
    protected Integer extendedWait;

    @Override
    public Integer getExtendedWait() {
        return extendedWait;
    }

    @Override
    public void setExtendedWait(Integer extendedWait) {
        super.setExtendedWait(extendedWait);
        this.extendedWait = extendedWait;
    }
}
