package ch.so.agi.jenkins.gretldatenportal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunDetailsTest {
    @Test
    void joinsLogLinesWithLineBreaks() {
        String text = RunDetails.joinLogLines(List.of(
                "Started by user admin",
                "[Pipeline] Start of Pipeline",
                "Finished: FAILURE"));

        assertEquals("Started by user admin\n[Pipeline] Start of Pipeline\nFinished: FAILURE", text);
    }

    @Test
    void returnsEmptyTextForMissingLogLines() {
        assertEquals("", RunDetails.joinLogLines(null));
    }

    @Test
    void readsCarriageReturnSeparatedLogLines() throws IOException {
        List<String> lines = RunDetails.readLogLines(
                new StringReader("Started by user admin\r[Pipeline] Start of Pipeline\rFinished: FAILURE"), 200);

        assertEquals(List.of(
                "Started by user admin",
                "[Pipeline] Start of Pipeline",
                "Finished: FAILURE"), lines);
    }

    @Test
    void keepsOnlyTailWhenReadingMixedLineBreaks() throws IOException {
        List<String> lines = RunDetails.readLogLines(new StringReader("first\r\nsecond\nthird\rfourth"), 2);

        assertEquals(List.of("third", "fourth"), lines);
    }
}
