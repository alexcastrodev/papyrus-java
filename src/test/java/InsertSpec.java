import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InsertSpec {

    @TempDir
    Path tempDir;

    private static Papyrus baseOf(Papyrus papyrus) {
        Papyrus base = new Papyrus();
        base.metadata = papyrus.metadata;
        base.status = Status.EMPTY;
        base.fragments = new HashMap<>();
        return base;
    }

    @Test
    void insertAddsFragmentAndMovesStatusFromEmptyToPartial() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 7);
        Path container = tempDir.resolve("book.papyrus");
        PapyrusFile.write(baseOf(papyrus), container);
        Path fragment = tempDir.resolve("page0.frag");
        FragmentFile.write(papyrus.fragments.get(0L), fragment);

        int exitCode = new CommandLine(new Insert()).execute(container.toString(), fragment.toString());

        assertEquals(0, exitCode);
        Papyrus updated = PapyrusFile.read(container);
        assertEquals(Status.PARTIAL, updated.status);
        assertArrayEquals(papyrus.fragments.get(0L).data, updated.fragments.get(0L).data);
    }

    @Test
    void insertingEveryFragmentCompletesThePapyrusAndAllowsExtraction() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 7);
        Path container = tempDir.resolve("book.papyrus");
        PapyrusFile.write(baseOf(papyrus), container);

        for (long page = 0; page < papyrus.metadata.totalPages; page++) {
            Path fragment = tempDir.resolve("page" + page + ".frag");
            FragmentFile.write(papyrus.fragments.get(page), fragment);
            int exitCode = new CommandLine(new Insert()).execute(container.toString(), fragment.toString());
            assertEquals(0, exitCode);
        }

        Papyrus updated = PapyrusFile.read(container);
        assertEquals(Status.COMPLETED, updated.status);
        assertArrayEquals(data, updated.extract());
    }

    @Test
    void insertIgnoresAFragmentWhosePageIsAlreadyPresent() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 7);
        Path container = tempDir.resolve("book.papyrus");
        PapyrusFile.write(baseOf(papyrus), container);
        Path fragment = tempDir.resolve("page0.frag");
        FragmentFile.write(papyrus.fragments.get(0L), fragment);
        new CommandLine(new Insert()).execute(container.toString(), fragment.toString());
        Papyrus afterFirstInsert = PapyrusFile.read(container);

        int exitCode = new CommandLine(new Insert()).execute(container.toString(), fragment.toString());

        assertEquals(0, exitCode);
        Papyrus afterSecondInsert = PapyrusFile.read(container);
        assertEquals(1, afterSecondInsert.fragments.size());
        assertEquals(afterFirstInsert.status, afterSecondInsert.status);
    }

    @Test
    void insertRejectsAFragmentWithTamperedData() throws IOException {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);
        Papyrus papyrus = Assemble.split(data, 7);
        Path container = tempDir.resolve("book.papyrus");
        PapyrusFile.write(baseOf(papyrus), container);
        Leaf original = papyrus.fragments.get(0L);
        Leaf tampered = new Leaf(original.page, original.totalPages, original.rootFingerprint, original.merkleProof, new byte[]{99});
        Path fragment = tempDir.resolve("tampered.frag");
        FragmentFile.write(tampered, fragment);

        int exitCode = new CommandLine(new Insert()).execute(container.toString(), fragment.toString());

        assertNotEquals(0, exitCode);
        Papyrus unchanged = PapyrusFile.read(container);
        assertEquals(Status.EMPTY, unchanged.status);
        assertEquals(0, unchanged.fragments.size());
    }
}
