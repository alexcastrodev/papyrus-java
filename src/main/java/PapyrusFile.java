import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// Serializa/deserializa um Papyrus para o formato de container `.papyrus` em disco.
//
// Layout (big-endian, via DataOutputStream/DataInputStream):
//   magic            4 bytes  "PAPY"
//   version          1 byte
//   metadata:
//     fingerprint      byte[]   (length-prefixed; -1 = null)
//     totalPages       long
//     fileName         string   (length-prefixed UTF-8; -1 = null)
//     signature        byte[]   (length-prefixed; -1 = null)
//     publicKey        byte[]   (length-prefixed; -1 = null)
//   status           byte     (ordinal de Status)
//   fragmentCount    int
//   fragmentCount x  Leaf:
//     page             long
//     totalPages       long
//     rootFingerprint  byte[]  (length-prefixed)
//     proofLength      int
//     proofLength x    byte[]  (length-prefixed, um hash irmão por nível)
//     data             byte[]  (length-prefixed)
class PapyrusFile {

    private static final byte[] MAGIC = {'P', 'A', 'P', 'Y'};
    private static final byte VERSION = 1;

    static void write(Papyrus papyrus, Path path) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);

        out.write(MAGIC);
        out.writeByte(VERSION);

        PapyrusMetadata metadata = papyrus.metadata;
        writeByteArray(out, metadata.fingerprint);
        out.writeLong(metadata.totalPages);
        writeString(out, metadata.fileName);
        writeByteArray(out, metadata.signature);
        writeByteArray(out, metadata.publicKey);
        out.writeByte(papyrus.status.ordinal());

        Map<Long, Leaf> fragments = papyrus.fragments != null ? papyrus.fragments : Map.of();
        out.writeInt(fragments.size());
        for (Leaf leaf : new TreeMap<>(fragments).values()) {
            out.writeLong(leaf.page);
            out.writeLong(leaf.totalPages);
            writeByteArray(out, leaf.rootFingerprint);
            out.writeInt(leaf.merkleProof.length);
            for (byte[] sibling : leaf.merkleProof) {
                writeByteArray(out, sibling);
            }
            writeByteArray(out, leaf.data);
        }

        Files.write(path, buffer.toByteArray());
    }

    static Papyrus read(Path path) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(path)));

        byte[] magic = new byte[MAGIC.length];
        in.readFully(magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            throw new IOException("not a papyrus file: " + path);
        }
        byte version = in.readByte();
        if (version != VERSION) {
            throw new IOException("unsupported papyrus version: " + version);
        }

        PapyrusMetadata metadata = new PapyrusMetadata();
        metadata.fingerprint = readByteArray(in);
        metadata.totalPages = in.readLong();
        metadata.fileName = readString(in);
        metadata.signature = readByteArray(in);
        metadata.publicKey = readByteArray(in);

        Papyrus papyrus = new Papyrus();
        papyrus.metadata = metadata;
        papyrus.status = Status.values()[in.readByte()];

        int fragmentCount = in.readInt();
        papyrus.fragments = new HashMap<>();
        for (int i = 0; i < fragmentCount; i++) {
            long page = in.readLong();
            long totalPages = in.readLong();
            byte[] rootFingerprint = readByteArray(in);
            int proofLength = in.readInt();
            byte[][] proof = new byte[proofLength][];
            for (int p = 0; p < proofLength; p++) {
                proof[p] = readByteArray(in);
            }
            byte[] data = readByteArray(in);
            papyrus.fragments.put(page, new Leaf(page, totalPages, rootFingerprint, proof, data));
        }

        return papyrus;
    }

    private static void writeByteArray(DataOutputStream out, byte[] array) throws IOException {
        if (array == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(array.length);
        out.write(array);
    }

    private static byte[] readByteArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        byte[] array = new byte[length];
        in.readFully(array);
        return array;
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        writeByteArray(out, value != null ? value.getBytes(StandardCharsets.UTF_8) : null);
    }

    private static String readString(DataInputStream in) throws IOException {
        byte[] bytes = readByteArray(in);
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }
}
