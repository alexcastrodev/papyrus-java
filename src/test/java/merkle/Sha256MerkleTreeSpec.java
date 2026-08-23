package merkle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sha256MerkleTreeSpec {

    private final MerkleTree merkle = new Sha256MerkleTree();

    @Test
    void singleLeafTreeHasRootEqualToLeafHash() {
        byte[] leaf = merkle.leafHash(0L, new byte[]{1, 2, 3});

        byte[] root = merkle.root(new byte[][]{leaf});

        assertArrayEquals(leaf, root);
    }

    @Test
    void singleLeafTreeHasEmptyProof() {
        byte[] leaf = merkle.leafHash(0L, new byte[]{1, 2, 3});

        byte[][] proof = merkle.proof(new byte[][]{leaf}, 0);

        assertTrue(proof.length == 0);
    }

    @Test
    void everyLeafVerifiesAgainstTheRootInAnEvenSizedTree() {
        byte[][] pages = {{1}, {2}, {3}, {4}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = merkle.leafHash(i, pages[i]);
        }
        byte[] root = merkle.root(leafHashes);

        for (int i = 0; i < pages.length; i++) {
            byte[][] proof = merkle.proof(leafHashes, i);
            assertTrue(merkle.verify(i, pages[i], proof, root), "page " + i + " should verify");
        }
    }

    @Test
    void everyLeafVerifiesAgainstTheRootInAnOddSizedTree() {
        byte[][] pages = {{1}, {2}, {3}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = merkle.leafHash(i, pages[i]);
        }
        byte[] root = merkle.root(leafHashes);

        for (int i = 0; i < pages.length; i++) {
            byte[][] proof = merkle.proof(leafHashes, i);
            assertTrue(merkle.verify(i, pages[i], proof, root), "page " + i + " should verify");
        }
    }

    @Test
    void verificationFailsWhenDataIsTampered() {
        byte[][] pages = {{1}, {2}, {3}, {4}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = merkle.leafHash(i, pages[i]);
        }
        byte[] root = merkle.root(leafHashes);
        byte[][] proof = merkle.proof(leafHashes, 2);

        assertFalse(merkle.verify(2, new byte[]{99}, proof, root));
    }

    @Test
    void verificationFailsWhenPageIndexIsSwapped() {
        byte[][] pages = {{1}, {2}, {3}, {4}};
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = merkle.leafHash(i, pages[i]);
        }
        byte[] root = merkle.root(leafHashes);
        byte[][] proof = merkle.proof(leafHashes, 1);

        // mesmos bytes, mas alegando ser a página 2 em vez da página 1
        assertFalse(merkle.verify(2, pages[1], proof, root));
    }
}
