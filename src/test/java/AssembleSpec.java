import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssembleSpec {

    @Test
    void splitFillsEveryPageAndTheResultIsAlreadyComplete() {
        byte[] data = "hello papyrus world".getBytes(StandardCharsets.UTF_8);

        Papyrus papyrus = Assemble.split(data, 5);

        assertEquals(4L, papyrus.metadata.totalPages);
        assertEquals(Status.COMPLETED, papyrus.status);
    }

    @Test
    void extractAfterSplitReturnsTheExactOriginalBytes() {
        byte[] data = "hello papyrus world, this spans more than one page".getBytes(StandardCharsets.UTF_8);

        Papyrus papyrus = Assemble.split(data, 7);

        assertArrayEquals(data, papyrus.extract());
    }

    @Test
    void splitOfDataSmallerThanOnePageProducesASingleLeafWithEmptyProof() {
        byte[] data = "hi".getBytes(StandardCharsets.UTF_8);

        Papyrus papyrus = Assemble.split(data, 4096);

        assertEquals(1L, papyrus.metadata.totalPages);
        assertEquals(0, papyrus.fragments.get(0L).merkleProof.length);
        assertArrayEquals(data, papyrus.extract());
    }

    @Test
    void splitRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> Assemble.split(new byte[0], 10));
    }
}
