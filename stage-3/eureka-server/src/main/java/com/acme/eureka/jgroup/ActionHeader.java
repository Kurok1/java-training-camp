package com.acme.eureka.jgroup;

import org.jgroups.Header;
import org.jgroups.conf.ClassConfigurator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ActionHeader extends Header {

    private byte action = -1;

    public static final short MAGIC_ID = 10240;

    static {
        ClassConfigurator.addIfAbsent(MAGIC_ID, ActionHeader.class);
    }

    public ActionHeader() {

    }

    public ActionHeader(byte action) {
        this.action = action;
    }

    @Override
    public short getMagicId() {
        return MAGIC_ID;
    }

    @Override
    public Supplier<? extends Header> create() {
        return ActionHeader::new;
    }

    @Override
    public int serializedSize() {
        return 1;
    }

    @Override
    public void writeTo(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(this.action);
    }

    @Override
    public void readFrom(DataInput dataInput) throws IOException, ClassNotFoundException {
        this.action = dataInput.readByte();
    }

    public byte getAction() {
        return action;
    }

    public void setAction(byte action) {
        this.action = action;
    }
}
