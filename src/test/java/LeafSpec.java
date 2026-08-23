import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LeafSpec {

    @Test
    void storesAllFieldsPassedToConstructor() {
        byte[] rootFingerprint = {1, 2, 3};
        byte[][] merkleProof = {{4, 5}, {6, 7}};
        byte[] data = {8, 9, 10};

        Leaf leaf = new Leaf(3L, 8L, rootFingerprint, merkleProof, data);

        assertEquals(3L, leaf.page);
        assertEquals(8L, leaf.totalPages);
        assertSame(rootFingerprint, leaf.rootFingerprint);
        assertSame(merkleProof, leaf.merkleProof);
        assertSame(data, leaf.data);
    }

    @Test
    void singleFragmentFileHasEmptyMerkleProof() {
        Leaf leaf = new Leaf(0L, 1L, new byte[]{1}, new byte[0][], new byte[]{9});

        assertEquals(0, leaf.merkleProof.length);
    }
}
