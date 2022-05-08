package gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.xml;

import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetSerializer;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetSerializerException;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.ListAlphabet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class AlphabetSerializerXml implements AlphabetSerializer {
    protected JAXBContext context;
    protected final AlphabetPojoXml alphabetPojoXml;

    protected synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (context == null) {
            context = JAXBContext.newInstance(alphabetPojoXml.getClass(), AbstractInput.class);
        }
        return context;
    }

    public AlphabetSerializerXml(AlphabetPojoXml alphabetPojoXml) {
        this.alphabetPojoXml = alphabetPojoXml;
    }

    public Alphabet<AbstractInput> read(InputStream alphabetStream) throws AlphabetSerializerException {
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader xsr = xif.createXMLStreamReader(new InputStreamReader(alphabetStream));
            AlphabetPojoXml alphabetPojoXml = (AlphabetPojoXml) unmarshaller.unmarshal(xsr);
            return new ListAlphabet<>(alphabetPojoXml.getInputs());

        } catch (JAXBException | XMLStreamException e) {
            throw new AlphabetSerializerException(e.getMessage());
        }
    }

    public void write(OutputStream alphabetStream, Alphabet<AbstractInput> alphabet) throws AlphabetSerializerException {
        try {
            Marshaller m = getJAXBContext().createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            AlphabetPojoXml alphabetPojo = alphabetPojoXml.getNewPojo(new ArrayList<>(alphabet));
            m.marshal(alphabetPojo, alphabetStream);
            
        } catch (JAXBException e) {
            throw new AlphabetSerializerException(e.getMessage());
        }
    }

}
