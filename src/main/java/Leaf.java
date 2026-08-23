class Leaf {
    Long page;
    Long totalPages;
    byte[] rootFingerprint;
    byte[][] merkleProof;
    byte[] data;

    public Leaf(Long page, Long totalPages, byte[] rootFingerprint, byte[][] merkleProof, byte[] data) {
        this.page = page;
        this.totalPages = totalPages;
        this.rootFingerprint = rootFingerprint;
        this.merkleProof = merkleProof;
        this.data = data;
    }
}
