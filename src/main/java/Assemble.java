import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(name = "assemble", mixinStandardHelpOptions = true,
        description = "Splits a file into Papyrus fragments backed by a Merkle tree.")
class Assemble implements Callable<Integer> {

    @Parameters(index = "0", description = "File to split.")
    private Path file;

    @Option(names = {"-p", "--page-size"}, description = "Page size in bytes (default: ${DEFAULT-VALUE}).")
    private int pageSize = 4096;

    @Option(names = {"-o", "--output"}, description = "Output .papyrus file (default: <file>.papyrus).")
    private Path output;

    static Papyrus split(byte[] data, int pageSize) {
        if (data.length == 0) {
            throw new IllegalArgumentException("cannot split an empty file");
        }

        int totalPages = (int) Math.ceil(data.length / (double) pageSize);
        byte[][] blocks = new byte[totalPages][];
        for (int page = 0; page < totalPages; page++) {
            int from = page * pageSize;
            int to = Math.min(from + pageSize, data.length);
            blocks[page] = Arrays.copyOfRange(data, from, to);
        }

        byte[][] leafHashes = new byte[totalPages][];
        for (int page = 0; page < totalPages; page++) {
            leafHashes[page] = MerkleTree.leafHash(page, blocks[page]);
        }
        byte[] root = MerkleTree.root(leafHashes);

        PapyrusMetadata metadata = new PapyrusMetadata();
        metadata.fingerprint = root;
        metadata.totalPages = (long) totalPages;

        Papyrus papyrus = new Papyrus();
        papyrus.metadata = metadata;
        papyrus.status = Status.EMPTY;

        for (int page = 0; page < totalPages; page++) {
            byte[][] proof = MerkleTree.proof(leafHashes, page);
            papyrus.addFragment(new Leaf((long) page, (long) totalPages, root, proof, blocks[page]));
        }

        return papyrus;
    }

    @Override
    public Integer call() {
        Path destination = output != null ? output : Path.of(file + ".papyrus");
        try {
            byte[] data = Files.readAllBytes(file);
            Papyrus papyrus = split(data, pageSize);
            papyrus.metadata.fileName = file.getFileName().toString();
            PapyrusFile.write(papyrus, destination);
            System.out.println("[Assemble] totalPages=" + papyrus.metadata.totalPages + " status=" + papyrus.status
                    + " -> " + destination.toAbsolutePath());
            return 0;
        } catch (IOException e) {
            System.out.println("[Assemble] Cannot read/write file: " + e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Assemble()).execute(args));
    }
}
