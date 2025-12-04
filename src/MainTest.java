import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.swing.*;
import java.lang.reflect.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class MainTest {
    //helper func to get private fields
    private <T> T getField(Main m, String name, Class<T> type) throws Exception {
        Field f = Main.class.getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(m));
    }
    //write to urlfile for test cases
    private void writeUrlsFile(String content) throws IOException {
        File urlsFile = new File("src/urls");
        urlsFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(urlsFile)) {
            writer.write(content);
        }
    }
    //for L3 test case
    private void deleteUrlsFile() {
        File urlsFile = new File("src/urls");
        if (urlsFile.exists()) {
            urlsFile.delete();
        }
    }
    @Test
    //test cases for loadPages:
    public void L1() throws Exception{
        //test num of urls
        writeUrlsFile("https://en.wikipedia.org/wiki/Test_automation\n" +
                "https://en.wikipedia.org/wiki/JUnit\n" +
                "https://en.wikipedia.org/wiki/Regression_testing");
        
        Main main = new Main();
        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        List<?> pages = getField(main, "pages", List.class);
        JTextArea results = getField(main,"results",JTextArea.class);
        //assert that wiki pages are there. Can change w respect to num of URLS
        assertEquals(3, dropdown.getItemCount());
        assertEquals(3, pages.size());
        assertEquals(" Select page and click Find Similar", results.getText());
    }
    @Test
    //empty url file test
    public void L2() throws Exception {
        writeUrlsFile("");
        Main main = new Main();
        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        List<?> pages = getField(main, "pages", List.class);
        JTextArea results = getField(main,"results",JTextArea.class);
        //asert there is no urls on file
        assertEquals(0, dropdown.getItemCount());
        assertEquals(0, pages.size());
        assertEquals(" Select page and click Find Similar", results.getText());
    }
    @Test
    //no url file present
    public void L3() throws Exception {
        deleteUrlsFile();
        Main main = new Main();
        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        List<?> pages = getField(main, "pages", List.class);
        JTextArea results = getField(main,"results",JTextArea.class);
        assertEquals(0, dropdown.getItemCount());
        assertEquals(0, pages.size());
        assertEquals("Error reading urls file", results.getText());
    }
    
    @Test
    //file with invalid URL
    public void L4() throws Exception {
        //mix of 2 valid urls and 1 invalid; only 2 should show
        writeUrlsFile("https://en.wikipedia.org/wiki/Test_automation\n" +
                "https://en.wikipedia.org/wiki/invalidurl\n" +
                "https://en.wikipedia.org/wiki/Regression_testing");

        //capture the error msg
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(result));

        Main main = new Main();
        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        List<?> pages = getField(main, "pages", List.class);
        assertEquals(2, dropdown.getItemCount());
        assertEquals(2, pages.size());
        String output = result.toString();
        assertTrue("Prints error to console", output.contains("Failed: https://en.wikipedia.org/wiki/invalidurl"));     
        System.setErr(origErr);
    }


    //tests for countWords method:
    @Test
    //should ignore numeric chars too
    public void CW1() {
        Main main = new Main();
        HT result = main.countWords("hello world 23 Hello hello 999 world");
        assertEquals(3, result.getCount("hello"));
        assertEquals(2, result.getCount("world"));
    }
    @Test
    public void CW2() {
        //empty string (boundary value)
        Main main = new Main();
        HT result = main.countWords("");
        assertEquals(0, result.getCount("anystring"));
    }
    @Test
    public void CW3() {
        Main main = new Main();
        HT result = main.countWords("The 123 and an 579");
        assertEquals(0, result.getCount("empty"));
    }
    @Test
    public void CW4() {
        //mixed cases and punctuation
        Main main = new Main();
        HT result = main.countWords("HeLLo! hello, world. ?HELLO world, wOrld!");
        assertEquals(3, result.getCount("world"));
        assertEquals(3, result.getCount("hello"));
    }
    //calculateTFIDF test cases:
    @Test
    public void TF1() throws Exception {
        //Normal calculation
        Main main = new Main();


        HT wordCounts = new HT();
        wordCounts.add("java");
        wordCounts.add("java");
        wordCounts.add("java");
        wordCounts.add("python");
        wordCounts.add("python");
        wordCounts.add("csharp");
        // 3 2 1 = 6
        HT wordInDoc = new HT();
        wordInDoc.add("java");
        wordInDoc.add("java");
        wordInDoc.add("python");
        wordInDoc.add("csharp");
        wordInDoc.add("csharp");
        wordInDoc.add("csharp");

        setField(main,"totalDocs",3);
        setField(main,"wordInDoc",wordInDoc);

        //tf = freq/totalWords, idf = log(totalDocs/docsWithWord)
        HT tfidf = main.calculateTFIDF(wordCounts);

        Double javaScore = (Double)  tfidf.get("java");
        Double pythonScore = (Double)  tfidf.get("python");
        Double csharpScore = (Double)  tfidf.get("csharp");

        assertNotNull("java score should not be null", javaScore);
        assertNotNull("python score should not be null", pythonScore);
        assertNotNull("csharp score should not be null", csharpScore);

        assertEquals(0.2027, javaScore, 0.001);
        assertEquals(0.3662, pythonScore, 0.001);
        assertEquals(0.0, csharpScore, 0.001);

    }
    @Test
    public void TF2() {
        //empty wordCount test
        Main main = new Main();

        HT wordCounts = new HT();
        HT tfidf = main.calculateTFIDF(wordCounts);
        assertEquals(0, tfidf.getCount(tfidf));

    }

    //similarity method test cases:
    @Test
    public void S1() {
        //identical vectors, non-empty HTs
        Main main = new Main();

        HT tfidf1 = new HT();
        tfidf1.add("apple",0.5);
        tfidf1.add("banana",0.3);
        tfidf1.add("orange",0.2);

        HT tfidf2 = new HT();
        tfidf2.add("apple",0.5);
        tfidf2.add("banana",0.3);
        tfidf2.add("orange",0.2);

        double similarity = main.similarity(tfidf1, tfidf2);

        assertEquals(1.0, similarity, 0.001);
    }
    @Test
    public void S2() {
        //empty HTs
        Main main = new Main();

        HT empty1 = new HT();
        HT empty2 = new HT();

        double similarity = main.similarity(empty1, empty2);

        assertEquals(0, similarity, 0.001);
    }

    @Test
    public void S3() {
        //orthogonal vectors, non-empty HTs
        Main main = new Main();

        HT tfidf1 = new HT();
        tfidf1.add("apple",0.5);
        tfidf1.add("banana",0.3);
        tfidf1.add("orange",0.2);

        HT tfidf2 = new HT();
        tfidf2.add("apple",0.2);
        tfidf2.add("banana",0.7);
        tfidf2.add("orange",0.5);

        double similarity = main.similarity(tfidf1, tfidf2);

        assertEquals(0.7530, similarity, 0.001);
    }

    //findSimilar method test cases:
    @Test
    public void FS1() throws Exception {
        //normal similarity check
        writeUrlsFile("https://en.wikipedia.org/wiki/Test_automation\n" +
            "https://en.wikipedia.org/wiki/JUnit\n" +
            "https://en.wikipedia.org/wiki/Regression_testing\n");

        Main main = new Main();
        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        List<?> pages = getField(main, "pages", List.class);
        JTextArea results = getField(main,"results",JTextArea.class);

        dropdown.setSelectedIndex(0);
        String selectedTitle = (String)dropdown.getSelectedItem();

        main.findSimilar();

        String out = results.getText();
        //recommended does not select itself
        assertFalse("findSimilar should not recommend selected page", out.contains(" 1. " + selectedTitle) || out.contains(" 2. " + selectedTitle));
    }
    @Test
    public void FS2() throws Exception {
        //self exclusion check: selected page itself is not in results
        writeUrlsFile("https://en.wikipedia.org/wiki/Test_automation\n" +
            "https://en.wikipedia.org/wiki/JUnit\n" +
            "https://en.wikipedia.org/wiki/Regression_testing\n");

        Main main = new Main();

        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        JTextArea results = getField(main, "results", JTextArea.class);
        List<?> pages = getField(main, "pages", List.class);

        assertEquals(2, pages.size());
    }
    @Test
    public void FS3() throws Exception {
        writeUrlsFile("");//empty url file

        Main main = new Main();

    }
    @Test
    public void FS4() throws Exception {
    //arrange: only one URL
        writeUrlsFile("https://en.wikipedia.org/wiki/JUnit\n");

        Main main = new Main();

        JComboBox<String> dropdown = getField(main, "dropdown", JComboBox.class);
        JTextArea results = getField(main, "results", JTextArea.class);
        List<?> pages = getField(main, "pages", List.class);

        //expect exactly 1 page loaded
        assertEquals(1, pages.size());
        assertEquals(1, dropdown.getItemCount());

        //select that only page
        dropdown.setSelectedIndex(0);

        main.findSimilar();

        String out = results.getText();

       //assert num "1." or "2." lines in results
       assertFalse(out.contains(" 1."));
       assertFalse(out.contains(" 2."));
}

}
