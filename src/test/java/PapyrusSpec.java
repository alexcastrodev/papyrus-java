import merkle.MerkleTree;
import merkle.Sha256MerkleTree;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PapyrusSpec {

    private static final MerkleTree MERKLE = new Sha256MerkleTree();

    @Test
    void freshInstanceHasNoFieldsSet() {
        Papyrus papyrus = new Papyrus();

        assertNull(papyrus.metadata);
        assertNull(papyrus.status);
        assertNull(papyrus.fragments);
    }

    @Test
    void storesFieldsAssignedAfterConstruction() {
        Papyrus papyrus = new Papyrus();
        PapyrusMetadata metadata = new PapyrusMetadata();
        byte[] fingerprint = {1, 2, 3};
        byte[] signature = {4, 5};
        byte[] publicKey = {6, 7};
        Map<Long, Leaf> fragments = new HashMap<>();

        metadata.fingerprint = fingerprint;
        metadata.totalPages = 8L;
        metadata.fileName = "book.pdf";
        metadata.signature = signature;
        metadata.publicKey = publicKey;
        papyrus.metadata = metadata;
        papyrus.status = Status.EMPTY;
        papyrus.fragments = fragments;

        assertSame(fingerprint, papyrus.metadata.fingerprint);
        assertEquals(8L, papyrus.metadata.totalPages);
        assertEquals("book.pdf", papyrus.metadata.fileName);
        assertSame(signature, papyrus.metadata.signature);
        assertSame(publicKey, papyrus.metadata.publicKey);
        assertEquals(Status.EMPTY, papyrus.status);
        assertSame(fragments, papyrus.fragments);
    }

    @Test
    void statusHasExactlyThreeLifecycleStages() {
        assertEquals(3, Status.values().length);
        assertEquals(Status.EMPTY, Status.valueOf("EMPTY"));
        assertEquals(Status.PARTIAL, Status.valueOf("PARTIAL"));
        assertEquals(Status.COMPLETED, Status.valueOf("COMPLETED"));
    }

    @Test
    void fragmentsMapKeysLeavesByPageNumber() {
        Papyrus papyrus = new Papyrus();
        papyrus.fragments = new HashMap<>();
        Leaf leaf = new Leaf(2L, 5L, new byte[]{1}, new byte[0][], new byte[]{9});

        papyrus.fragments.put(leaf.page, leaf);

        assertTrue(papyrus.fragments.containsKey(2L));
        assertSame(leaf, papyrus.fragments.get(2L));
    }

    private static Papyrus emptyPapyrusFor(byte[][] pages) {
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = MERKLE.leafHash(i, pages[i]);
        }

        PapyrusMetadata metadata = new PapyrusMetadata();
        metadata.fingerprint = MERKLE.root(leafHashes);
        metadata.totalPages = (long) pages.length;

        Papyrus papyrus = new Papyrus();
        papyrus.metadata = metadata;
        papyrus.status = Status.EMPTY;
        return papyrus;
    }

    private static Leaf leafFor(byte[][] pages, int page) {
        byte[][] leafHashes = new byte[pages.length][];
        for (int i = 0; i < pages.length; i++) {
            leafHashes[i] = MERKLE.leafHash(i, pages[i]);
        }
        byte[] root = MERKLE.root(leafHashes);
        byte[][] proof = MERKLE.proof(leafHashes, page);
        return new Leaf((long) page, (long) pages.length, root, proof, pages[page]);
    }

    @Test
    void statusMovesFromEmptyToPartialToCompletedAsFragmentsArrive() {
        byte[][] pages = {{1}, {2}, {3}};
        Papyrus papyrus = emptyPapyrusFor(pages);

        assertEquals(Status.EMPTY, papyrus.status);

        papyrus.addFragment(leafFor(pages, 0));
        assertEquals(Status.PARTIAL, papyrus.status);

        papyrus.addFragment(leafFor(pages, 1));
        assertEquals(Status.PARTIAL, papyrus.status);

        papyrus.addFragment(leafFor(pages, 2));
        assertEquals(Status.COMPLETED, papyrus.status);
    }

    @Test
    void addFragmentRejectsLeafFromADifferentPapyrus() {
        byte[][] pages = {{1}, {2}};
        Papyrus papyrus = emptyPapyrusFor(pages);
        Leaf foreignLeaf = leafFor(new byte[][]{{9}, {8}}, 0);

        assertThrows(IllegalArgumentException.class, () -> papyrus.addFragment(foreignLeaf));
    }

    @Test
    void addFragmentRejectsLeafWithTamperedData() {
        byte[][] pages = {{1}, {2}, {3}};
        Papyrus papyrus = emptyPapyrusFor(pages);
        Leaf leaf = leafFor(pages, 1);
        Leaf tampered = new Leaf(leaf.page, leaf.totalPages, leaf.rootFingerprint, leaf.merkleProof, new byte[]{99});

        assertThrows(IllegalArgumentException.class, () -> papyrus.addFragment(tampered));
    }

    @Test
    void extractThrowsWhenPapyrusIsNotComplete() {
        byte[][] pages = {{1}, {2}};
        Papyrus papyrus = emptyPapyrusFor(pages);
        papyrus.addFragment(leafFor(pages, 0));

        assertThrows(IllegalStateException.class, papyrus::extract);
    }

    @Test
    void extractReassemblesTheOriginalBytesInPageOrderRegardlessOfArrivalOrder() {
        byte[][] pages = {{1, 1}, {2, 2}, {3, 3}, {4, 4}};
        Papyrus papyrus = emptyPapyrusFor(pages);

        // adiciona fora de ordem de propósito
        papyrus.addFragment(leafFor(pages, 2));
        papyrus.addFragment(leafFor(pages, 0));
        papyrus.addFragment(leafFor(pages, 3));
        papyrus.addFragment(leafFor(pages, 1));

        assertEquals(Status.COMPLETED, papyrus.status);
        assertArrayEquals(new byte[]{1, 1, 2, 2, 3, 3, 4, 4}, papyrus.extract());
    }
}
