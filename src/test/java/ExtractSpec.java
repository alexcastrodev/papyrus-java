import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractSpec {

    @TempDir
    Path tempDir;

    @Test
    void extractWritesTheFileUnderItsOriginalStoredName() throws IOException {
        byte[] data = "hello papyrus world".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 5);
        papyrus.metadata.fileName = "book.pdf";
        Path container = tempDir.resolve("book.papyrus");
        PapyrusFile.write(papyrus, container);

        int exitCode = new CommandLine(new Extract()).execute(container.toString());

        assertEquals(0, exitCode);
        Path expected = tempDir.resolve("book.pdf");
        assertTrue(Files.exists(expected));
        assertArrayEquals(data, Files.readAllBytes(expected));
    }

    @Test
    void extractFallsBackToAGenericNameWhenOriginalNameWasNotStored() throws IOException {
        byte[] data = "no name stored".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 5);
        Path container = tempDir.resolve("mystery.papyrus");
        PapyrusFile.write(papyrus, container);

        int exitCode = new CommandLine(new Extract()).execute(container.toString());

        assertEquals(0, exitCode);
        Path expected = tempDir.resolve("mystery.papyrus.out");
        assertTrue(Files.exists(expected));
        assertArrayEquals(data, Files.readAllBytes(expected));
    }

    @Test
    void extractHonorsExplicitOutputOption() throws IOException {
        byte[] data = "explicit output".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 5);
        papyrus.metadata.fileName = "ignored.txt";
        Path container = tempDir.resolve("x.papyrus");
        PapyrusFile.write(papyrus, container);
        Path destination = tempDir.resolve("custom-name.bin");

        int exitCode = new CommandLine(new Extract()).execute(container.toString(), "-o", destination.toString());

        assertEquals(0, exitCode);
        assertArrayEquals(data, Files.readAllBytes(destination));
    }
}
