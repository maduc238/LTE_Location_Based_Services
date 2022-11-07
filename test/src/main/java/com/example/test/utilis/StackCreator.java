package com.example.test.utilis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Mode;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;


public class StackCreator extends StackImpl {
    
    private static Logger logger = Logger.getLogger(StackCreator.class);
    private Stack stack = null;

    // Constructor voi input config
    public StackCreator(Configuration config, String identifier) {
        super();
        this.stack = new StackImpl(); //Khai bao stack

        try {
            this.stack.init(config); // Khai tao stack voi cau hinh cho truoc

            Thread.sleep(500);
        } catch (Exception e) {
            // TODO: handle exception
            logger.error("Failure creating stack '" + identifier + "'", e);
        }
    }

    //Constructor voi input tu file
    public StackCreator(InputStream streamConfig, String dooer) throws Exception {
    this(new XMLConfiguration(streamConfig), dooer);
    }

    //Constructor voi input tu string
    public StackCreator(String stringConfig, String dooer)
        throws Exception {
    this(new XMLConfiguration(new ByteArrayInputStream(stringConfig.getBytes())), dooer);
    }

    @Override
    public void destroy() {
    stack.destroy();
    }

    @Override
    public java.util.logging.Logger getLogger() {
    return stack.getLogger();
    }

    @Override
    public MetaData getMetaData() {
    return stack.getMetaData();
    }

    @Override
    public SessionFactory getSessionFactory() throws IllegalDiameterStateException {
    return stack.getSessionFactory();
    }

    @Override
    public SessionFactory init(Configuration config) throws IllegalDiameterStateException, InternalException {
    return stack.init(config);
    }

    @Override
    public boolean isActive() {
    return stack.isActive();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws InternalException {
    return stack.isWrapperFor(iface);
    }

    @Override
    public void start() throws IllegalDiameterStateException, InternalException {
    stack.start();
    }

    @Override
    public void start(Mode mode, long timeout, TimeUnit unit) throws IllegalDiameterStateException, InternalException {
    stack.start(mode, timeout, unit);
    }

    @Override
    public void stop(long timeout, TimeUnit unit, int disconnectReason) throws IllegalDiameterStateException, InternalException {
    stack.stop(timeout, unit, disconnectReason);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws InternalException {
    return stack.unwrap(iface);
    }
}
