package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Concretizer {
    private static final Logger LOGGER = LogManager.getLogger();
    protected int recordLength;
    protected FileWriter fileWriter;
    protected PrintWriter printWriter;
    protected FileOutputStream fosRep;
    protected FileOutputStream fosRaw;

    public Concretizer(String path, String name) {
        try {
            if (path.equals("")) path = ".";
            this.recordLength = 0;
            File folder = new File(path);
            if (!folder.exists()) {
                if(!folder.mkdir()) {
                    LOGGER.error("Cannot create folder");
                    return;
                }
            }
            this.fileWriter = new FileWriter(new File(folder, name + ".length"), StandardCharsets.UTF_8);
            this.printWriter = new PrintWriter(fileWriter);
            this.fosRep = new FileOutputStream(new File(folder, name + ".replay"), false);
            this.fosRaw = new FileOutputStream(new File(folder, name + ".raw"), false);
        } catch (IOException e) {
            LOGGER.error("Cannot create files");
        }
    }

    public void concretize(byte[] val) {
        try {
            if (val == null) {
                LOGGER.error("Message to concretize is null");
                return;
            }
            recordLength += 1;
            byte[] len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(val.length).array();
            fosRep.write(len);
            fosRep.write(val);
            fosRaw.write(val);
        } catch (IOException e) {
            LOGGER.error("Cannot write files");
        }
    }

    public void close() {
        try {
            printWriter.println(recordLength);
            fileWriter.close();
            printWriter.close();
            fosRep.close();
            fosRaw.close();
        } catch (IOException e) {
            LOGGER.error("Cannot close files");
        }
    }
}
