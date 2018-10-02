package fr.unistra.bioinfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public abstract class CustomTestCase {
    protected static Logger LOGGER;
    static{
        System.setProperty("log4j.configurationFile","log4j2-test.xml");
        LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    }
}
