package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Concretizer {
    protected int recordLength;
    protected FileWriter fileWriter;
    protected PrintWriter printWriter;
    protected FileOutputStream fosRep;
    protected FileOutputStream fosRaw;

    public Concretizer(String path, String name) {
        try {
            if (path.equals("")) path = ".";
            this.recordLength = 0;
            this.fileWriter = new FileWriter(new File(path, name + ".length"), StandardCharsets.UTF_8);
            this.printWriter = new PrintWriter(fileWriter);
            this.fosRep = new FileOutputStream(new File(path, name + ".replay"), true);
            this.fosRaw = new FileOutputStream(new File(path, name + ".raw"), true);
        } catch (IOException e) {
            ;
        }
    }

    public void concretize(byte[] val) {
        try {
            if (val == null) throw new IOException();
            recordLength += 1;
            byte[] len = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(val.length).array();
            fosRep.write(len);
            fosRep.write(val);
            fosRaw.write(val);
        } catch (IOException e) {
            ;
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
            ;
        }
    }
}
