package merkle;

public interface MerkleTree {

    byte[] leafHash(long page, byte[] data);

    byte[] root(byte[][] leafHashes);

    byte[][] proof(byte[][] leafHashes, int index);

    boolean verify(long page, byte[] data, byte[][] proof, byte[] root);
}
