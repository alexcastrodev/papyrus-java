import java.util.Map;

class Papyrus {
    byte[] fingerprint;
    Long totalPages;
    byte[] signature;
    byte[] publicKey;
    Status status;
    Map<Long, Leaf> fragments;
}

enum Status {
    EMPTY, PARTIAL, COMPLETED
}
