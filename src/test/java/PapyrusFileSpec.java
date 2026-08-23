import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PapyrusFileSpec {

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadRoundTripsEveryField() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus original = Assemble.split(data, 7);
        Path path = tempDir.resolve("book.papyrus");

        PapyrusFile.write(original, path);
        Papyrus loaded = PapyrusFile.read(path);

        assertArrayEquals(original.metadata.fingerprint, loaded.metadata.fingerprint);
        assertEquals(original.metadata.totalPages, loaded.metadata.totalPages);
        assertEquals(original.status, loaded.status);
        assertEquals(original.fragments.size(), loaded.fragments.size());
        for (long page = 0; page < original.metadata.totalPages; page++) {
            Leaf originalLeaf = original.fragments.get(page);
            Leaf loadedLeaf = loaded.fragments.get(page);
            assertArrayEquals(originalLeaf.data, loadedLeaf.data);
            assertArrayEquals(originalLeaf.rootFingerprint, loadedLeaf.rootFingerprint);
            assertEquals(originalLeaf.merkleProof.length, loadedLeaf.merkleProof.length);
            for (int i = 0; i < originalLeaf.merkleProof.length; i++) {
                assertArrayEquals(originalLeaf.merkleProof[i], loadedLeaf.merkleProof[i]);
            }
        }
    }

    @Test
    void loadedPapyrusExtractsToTheOriginalBytes() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus original = Assemble.split(data, 7);
        Path path = tempDir.resolve("book.papyrus");

        PapyrusFile.write(original, path);
        Papyrus loaded = PapyrusFile.read(path);

        assertArrayEquals(data, loaded.extract());
    }

    @Test
    void nullSignatureAndPublicKeySurviveAsNull() throws IOException {
        Papyrus original = Assemble.split("hi".getBytes(StandardCharsets.UTF_8), 4096);
        Path path = tempDir.resolve("no-signature.papyrus");

        PapyrusFile.write(original, path);
        Papyrus loaded = PapyrusFile.read(path);

        assertNull(loaded.metadata.signature);
        assertNull(loaded.metadata.publicKey);
    }

    @Test
    void fileNameSurvivesTheRoundTrip() throws IOException {
        Papyrus original = Assemble.split("hi".getBytes(StandardCharsets.UTF_8), 4096);
        original.metadata.fileName = "book.pdf";
        Path path = tempDir.resolve("with-name.papyrus");

        PapyrusFile.write(original, path);
        Papyrus loaded = PapyrusFile.read(path);

        assertEquals("book.pdf", loaded.metadata.fileName);
    }

    @Test
    void missingFileNameSurvivesAsNull() throws IOException {
        Papyrus original = Assemble.split("hi".getBytes(StandardCharsets.UTF_8), 4096);
        Path path = tempDir.resolve("without-name.papyrus");

        PapyrusFile.write(original, path);
        Papyrus loaded = PapyrusFile.read(path);

        assertNull(loaded.metadata.fileName);
    }

    @Test
    void readRejectsAFileWithoutTheMagicHeader() throws IOException {
        Path path = tempDir.resolve("not-a-papyrus.bin");
        Files.write(path, new byte[]{1, 2, 3, 4, 5});

        assertThrows(IOException.class, () -> PapyrusFile.read(path));
    }

    @Test
    void readRejectsAnUnsupportedVersion() throws IOException {
        Path path = tempDir.resolve("future-version.papyrus");
        Files.write(path, new byte[]{'P', 'A', 'P', 'Y', 99});

        assertThrows(IOException.class, () -> PapyrusFile.read(path));
    }
}
