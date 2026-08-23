import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PapyrusSpec {

    @Test
    void freshInstanceHasNoFieldsSet() {
        Papyrus papyrus = new Papyrus();

        assertNull(papyrus.fingerprint);
        assertNull(papyrus.totalPages);
        assertNull(papyrus.signature);
        assertNull(papyrus.publicKey);
        assertNull(papyrus.status);
        assertNull(papyrus.fragments);
    }

    @Test
    void storesFieldsAssignedAfterConstruction() {
        Papyrus papyrus = new Papyrus();
        byte[] fingerprint = {1, 2, 3};
        byte[] signature = {4, 5};
        byte[] publicKey = {6, 7};
        Map<Long, Leaf> fragments = new HashMap<>();

        papyrus.fingerprint = fingerprint;
        papyrus.totalPages = 8L;
        papyrus.signature = signature;
        papyrus.publicKey = publicKey;
        papyrus.status = Status.EMPTY;
        papyrus.fragments = fragments;

        assertSame(fingerprint, papyrus.fingerprint);
        assertEquals(8L, papyrus.totalPages);
        assertSame(signature, papyrus.signature);
        assertSame(publicKey, papyrus.publicKey);
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
}
