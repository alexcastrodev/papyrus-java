import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Serializa/deserializa um único Leaf como arquivo de fragmento avulso (formato `.frag`).
//
// Layout (big-endian, via DataOutputStream/DataInputStream):
//   magic            4 bytes  "PFRG"
//   version          1 byte
//   page             long
//   totalPages       long
//   rootFingerprint  byte[]  (length-prefixed)
//   proofLength      int
//   proofLength x    byte[]  (length-prefixed, um hash irmão por nível)
//   data             byte[]  (length-prefixed)
class FragmentFile {

    private static final byte[] MAGIC = {'P', 'F', 'R', 'G'};
    private static final byte VERSION = 1;

    static void write(Leaf leaf, Path path) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);

        out.write(MAGIC);
        out.writeByte(VERSION);
        out.writeLong(leaf.page);
        out.writeLong(leaf.totalPages);
        PapyrusFile.writeByteArray(out, leaf.rootFingerprint);
        out.writeInt(leaf.merkleProof.length);
        for (byte[] sibling : leaf.merkleProof) {
            PapyrusFile.writeByteArray(out, sibling);
        }
        PapyrusFile.writeByteArray(out, leaf.data);

        Files.write(path, buffer.toByteArray());
    }

    static Leaf read(Path path) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(path)));

        byte[] magic = new byte[MAGIC.length];
        in.readFully(magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            throw new IOException("not a papyrus fragment file: " + path);
        }
        byte version = in.readByte();
        if (version != VERSION) {
            throw new IOException("unsupported fragment version: " + version);
        }

        long page = in.readLong();
        long totalPages = in.readLong();
        byte[] rootFingerprint = PapyrusFile.readByteArray(in);
        int proofLength = in.readInt();
        byte[][] proof = new byte[proofLength][];
        for (int p = 0; p < proofLength; p++) {
            proof[p] = PapyrusFile.readByteArray(in);
        }
        byte[] data = PapyrusFile.readByteArray(in);

        return new Leaf(page, totalPages, rootFingerprint, proof, data);
    }
}
