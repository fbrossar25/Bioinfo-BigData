package fr.unistra.bioinfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;

import java.lang.invoke.MethodHandles;

public class CustomTestCase {
    protected static Logger LOGGER = null;
    static{
        System.setProperty("log4j.configurationFile","log4j2-test.xml");
    }

    @BeforeAll
    public static void beforeAll(){
        LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    }
}
