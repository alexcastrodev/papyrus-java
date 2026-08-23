import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FragmentFileSpec {

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadRoundTripsEveryField() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 7);
        Leaf original = papyrus.fragments.get(2L);
        Path path = tempDir.resolve("fragment.frag");

        FragmentFile.write(original, path);
        Leaf loaded = FragmentFile.read(path);

        assertEquals(original.page, loaded.page);
        assertEquals(original.totalPages, loaded.totalPages);
        assertArrayEquals(original.rootFingerprint, loaded.rootFingerprint);
        assertArrayEquals(original.data, loaded.data);
        assertEquals(original.merkleProof.length, loaded.merkleProof.length);
        for (int i = 0; i < original.merkleProof.length; i++) {
            assertArrayEquals(original.merkleProof[i], loaded.merkleProof[i]);
        }
    }

    @Test
    void writeThenReadRoundTripsASingleLeafWithEmptyProof() throws IOException {
        Papyrus papyrus = Assemble.split("hi".getBytes(StandardCharsets.UTF_8), 4096);
        Leaf original = papyrus.fragments.get(0L);
        Path path = tempDir.resolve("single.frag");

        FragmentFile.write(original, path);
        Leaf loaded = FragmentFile.read(path);

        assertEquals(0, loaded.merkleProof.length);
        assertArrayEquals(original.data, loaded.data);
    }

    @Test
    void readRejectsAFileWithoutTheMagicHeader() throws IOException {
        Path path = tempDir.resolve("not-a-fragment.bin");
        Files.write(path, new byte[]{1, 2, 3, 4, 5});

        assertThrows(IOException.class, () -> FragmentFile.read(path));
    }

    @Test
    void readRejectsAnUnsupportedVersion() throws IOException {
        Path path = tempDir.resolve("future-version.frag");
        Files.write(path, new byte[]{'P', 'F', 'R', 'G', 99});

        assertThrows(IOException.class, () -> FragmentFile.read(path));
    }
}
