package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.Main;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

class GenbankParserTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Test
    void parseGenbankFile() {
        Path p = Paths.get(".","src", "test", "resources", "NC_001700.1.gb");
        GenbankParser.parseGenbankFile(p.toFile());
    }
}