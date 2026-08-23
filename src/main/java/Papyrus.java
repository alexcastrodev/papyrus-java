import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

class Papyrus {
    PapyrusMetadata metadata;
    Status status;
    Map<Long, Leaf> fragments;

    void addFragment(Leaf leaf) {
        if (!java.util.Arrays.equals(leaf.rootFingerprint, metadata.fingerprint) || !leaf.totalPages.equals(metadata.totalPages)) {
            throw new IllegalArgumentException("leaf does not belong to this papyrus");
        }
        if (!MerkleTree.verify(leaf.page, leaf.data, leaf.merkleProof, metadata.fingerprint)) {
            throw new IllegalArgumentException("invalid Merkle proof for page " + leaf.page);
        }

        if (fragments == null) {
            fragments = new HashMap<>();
        }
        fragments.put(leaf.page, leaf);
        status = (fragments.size() == metadata.totalPages) ? Status.COMPLETED : Status.PARTIAL;
    }

    byte[] extract() {
        if (status != Status.COMPLETED) {
            throw new IllegalStateException("papyrus is not complete: " + status);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (long page = 0; page < metadata.totalPages; page++) {
            out.writeBytes(fragments.get(page).data);
        }
        return out.toByteArray();
    }
}

enum Status {
    EMPTY, PARTIAL, COMPLETED
}
