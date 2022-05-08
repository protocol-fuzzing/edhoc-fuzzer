package gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.xml;

import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * POJO class used for .xml de-serialization.
 */
@XmlRootElement(name = "alphabet")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AlphabetPojoXml {
    /*
     * To be extended with class annotating variable inputs like this:
     *  @XmlElements(value = {
     *		@XmlElement(type = InputA.class, name = "InputA"),
     *		@XmlElement(type = InputB.class, name = "InputB"),
     *		...
     *	})
     * where InputX.class is the corresponding java class to xml element in alphabet file
     *
     * Example of such class:

     public class AlphabetPojoXmlExt extends AlphabetPojoXml {
         @XmlElements(value = {
            @XmlElement(type = InputA.class, name = "InputA"),
            @XmlElement(type = InputB.class, name = "InputB")
         })
         protected List<AbstractInput> inputs;

         public AlphabetPojoExt AlphabetPojoXmlExt(List<AbstractInput> inputs) {
            this.inputs = inputs;
         }

         public AlphabetPojoXml getNewPojo(List<AbstractInput> inputs) {
            return new AlphabetPojoXmlExt(inputs);
         }

         public List<AbstractInput> getInputs(){
            return inputs;
         }
      }

     *
     */

    public AlphabetPojoXml getNewPojo(List<AbstractInput> inputs) {
        return null;
    }

    public List<AbstractInput> getInputs(){
        return null;
    }
}

