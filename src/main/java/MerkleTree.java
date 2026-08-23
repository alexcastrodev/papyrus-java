import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MerkleTree {

    static byte[] leafHash(long page, byte[] data) {
        return sha256(longToBytes(page), data);
    }

    static byte[] root(byte[][] leafHashes) {
        if (leafHashes.length == 0) {
            throw new IllegalArgumentException("cannot build a Merkle tree with no leaves");
        }
        List<byte[][]> levels = levels(leafHashes);
        return levels.get(levels.size() - 1)[0];
    }

    static byte[][] proof(byte[][] leafHashes, int index) {
        List<byte[][]> levels = levels(leafHashes);
        List<byte[]> path = new ArrayList<>();
        int idx = index;
        for (int level = 0; level < levels.size() - 1; level++) {
            byte[][] nodes = levels.get(level);
            int siblingIndex = (idx % 2 == 0) ? idx + 1 : idx - 1;
            byte[] sibling = (siblingIndex < nodes.length) ? nodes[siblingIndex] : nodes[idx];
            path.add(sibling);
            idx /= 2;
        }
        return path.toArray(new byte[0][]);
    }

    static boolean verify(long page, byte[] data, byte[][] proof, byte[] root) {
        byte[] computed = leafHash(page, data);
        for (byte[] sibling : proof) {
            computed = pairHash(computed, sibling);
        }
        return Arrays.equals(computed, root);
    }

    private static List<byte[][]> levels(byte[][] leafHashes) {
        if (leafHashes.length == 0) {
            throw new IllegalArgumentException("cannot build a Merkle tree with no leaves");
        }
        List<byte[][]> levels = new ArrayList<>();
        byte[][] current = leafHashes;
        levels.add(current);
        while (current.length > 1) {
            byte[][] next = new byte[(current.length + 1) / 2][];
            for (int i = 0; i < next.length; i++) {
                byte[] left = current[i * 2];
                byte[] right = (i * 2 + 1 < current.length) ? current[i * 2 + 1] : left;
                next[i] = pairHash(left, right);
            }
            levels.add(next);
            current = next;
        }
        return levels;
    }

    // Par ordenado antes de hashear: torna o hash comutativo (pairHash(a,b) == pairHash(b,a)),
    // então proof/verify não precisam rastrear se cada nó é filho esquerdo ou direito.
    private static byte[] pairHash(byte[] left, byte[] right) {
        return compare(left, right) <= 0 ? sha256(left, right) : sha256(right, left);
    }

    private static int compare(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int diff = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (diff != 0) return diff;
        }
        return a.length - b.length;
    }

    private static byte[] sha256(byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] part : parts) {
                digest.update(part);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[Long.BYTES];
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }
}
