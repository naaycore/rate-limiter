package com.prodigious;

import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.prodigious.Util.retrieveEndpointConfigurations;
import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    private TestLogAppender logAppender;

    @BeforeEach
    public void setup() {
        logAppender = new TestLogAppender();
        logAppender.start();
        Logger log = (Logger)LoggerFactory.getLogger(Util.class);
        log.addAppender(logAppender);
    }

    @Test
    void testReadFile_createsStringOfFilePathContent() {
        String path = "src/test/resources/test.txt";
        String content = Util.readFile(path);
        assertEquals("This is a test file\nwith a carriage return", content);
    }

    @Test
    void testReadFile_FileDoesNotExist() {
        String path = "src/test/resources/test_ghost.txt";
        Exception exception = assertThrows(
                RuntimeException.class, () -> Util.readFile(path)

        );

        String expectedMessage =
                "java.nio.file.NoSuchFileException: src/test/resources/test_ghost.txt";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testRetrieveEndpointConfigurations_nullInput() {
        String path = null;

        Exception exception = assertThrows(
                NullPointerException.class,
                () -> retrieveEndpointConfigurations(path)
        );

        assertNull(exception.getMessage());
    }

    @Test
    void testRetrieveEndpointConfiguration_noFilesInDirectory(){
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/emptyDirectory");
        assertEquals(0, map.size());
    }

    @Test
    void testRetrieveEndpointConfiguration_ignoreNonJsonFiles() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/nonJson");
        String logs = logAppender.getLogs();

        assertTrue(logs.contains("File test.jsn is not a json file, ignoring"));
        assertEquals(0, map.size());
    }

    @Test
    void testRetrieveEndpointConfiguration_noFileExtension() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/noExtension");
        String logs = logAppender.getLogs();

        assertTrue(logs.contains("File file is not a json file, ignoring"));
        assertEquals(0, map.size());
    }

    @Test
    void testRetrieveEndpointConfiguration_folderWithJsonFiles() {
        Map<String, String> map = Util.retrieveEndpointConfigurations(
                "src/test/resources/jsonFile");

        assertEquals(1, map.size());
    }
}