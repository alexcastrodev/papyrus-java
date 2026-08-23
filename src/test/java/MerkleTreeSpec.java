import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerkleTreeSpec {

    @Test
    void singleLeafTreeHasRootEqualToLeafHash() {
        byte[] leaf = MerkleTree.leafHash(0L, new byte[]{1, 2, 3});

        byte[] root = MerkleTree.root(new byte[][]{leaf});

        assertArrayEquals(leaf, root);
    }

    @Test
    void singleLeafTreeHasEmptyProof() {
        byte[] leaf = MerkleTree.leafHash(0L, new byte[]{1, 2, 3});

        byte[][] proof = MerkleTree.proof(new byte[][]{leaf}, 0);

        assertTrue(proof.length == 0);
    }

    @Test
    void everyLeafVerifiesAgainstTheRootInAnEvenSizedTree() {
        byte[][] pages = {{1}, {2}, {3}, {4}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = MerkleTree.leafHash(i, pages[i]);
        }
        byte[] root = MerkleTree.root(leafHashes);

        for (int i = 0; i < pages.length; i++) {
            byte[][] proof = MerkleTree.proof(leafHashes, i);
            assertTrue(MerkleTree.verify(i, pages[i], proof, root), "page " + i + " should verify");
        }
    }

    @Test
    void everyLeafVerifiesAgainstTheRootInAnOddSizedTree() {
        byte[][] pages = {{1}, {2}, {3}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = MerkleTree.leafHash(i, pages[i]);
        }
        byte[] root = MerkleTree.root(leafHashes);

        for (int i = 0; i < pages.length; i++) {
            byte[][] proof = MerkleTree.proof(leafHashes, i);
            assertTrue(MerkleTree.verify(i, pages[i], proof, root), "page " + i + " should verify");
        }
    }

    @Test
    void verificationFailsWhenDataIsTampered() {
        byte[][] pages = {{1}, {2}, {3}, {4}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = MerkleTree.leafHash(i, pages[i]);
        }
        byte[] root = MerkleTree.root(leafHashes);
        byte[][] proof = MerkleTree.proof(leafHashes, 2);

        assertFalse(MerkleTree.verify(2, new byte[]{99}, proof, root));
    }

    @Test
    void verificationFailsWhenPageIndexIsSwapped() {
        byte[][] pages = {{1}, {2}, {3}, {4}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = MerkleTree.leafHash(i, pages[i]);
        }
        byte[] root = MerkleTree.root(leafHashes);
        byte[][] proof = MerkleTree.proof(leafHashes, 1);

        // mesmos bytes, mas alegando ser a página 2 em vez da página 1
        assertFalse(MerkleTree.verify(2, pages[1], proof, root));
    }
}
