package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.protocol.ProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutputChecker;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContext;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.InputMapper;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class EdhocInputMapper extends InputMapper {
    EdhocMapperConnector edhocMapperConnector;

    public EdhocInputMapper(MapperConfig mapperConfig, AbstractOutputChecker outputChecker,
                            EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig, outputChecker);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    protected void sendMessage(ProtocolMessage message, ExecutionContext context) {
        if (message == null) {
            throw new RuntimeException("Null message provided to EdhocInputMapper in sendMessage");
        }

        EdhocProtocolMessage edhocProtocolMessage = (EdhocProtocolMessage) message;

        // enable or disable content format
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) mapperConfig;
        int contentFormat = edhocMapperConfig.useContentFormat() ? edhocProtocolMessage.getContentFormat() : MediaTypeRegistry.UNDEFINED;

        try {
            File fileReader = new File("send.length");
            int recordLength = 0;
            if(fileReader.exists()) {
                Scanner scanner = new Scanner(fileReader, StandardCharsets.UTF_8);
                recordLength = scanner.nextInt();
            }
            recordLength += 1;
            FileWriter fileWriter = new FileWriter("send.length", StandardCharsets.UTF_8);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(recordLength+"\n");
            fileWriter.close();
            printWriter.close();
            byte[] val = edhocProtocolMessage.getPayload();
            byte[] len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(val.length).array();
            FileOutputStream fosRep = new FileOutputStream("send.replay",true);
            if (val.length > 0) fosRep.write(len);
            fosRep.write(val);
            fosRep.close();
            FileOutputStream fosRaw = new FileOutputStream("send.raw",true);
            fosRaw.write(val);
            fosRaw.close();
        } catch (IOException e) {
            ;
        }

        edhocMapperConnector.send(edhocProtocolMessage.getPayload(), edhocProtocolMessage.getPayloadType(),
                edhocProtocolMessage.getMessageCode(), contentFormat);
    }
}
