import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "extract", mixinStandardHelpOptions = true,
        description = "Reassembles the original file from a .papyrus container.")
class Extract implements Callable<Integer> {

    @Parameters(index = "0", description = ".papyrus file to reassemble.")
    private Path file;

    @Option(names = {"-o", "--output"}, description = "Output file (default: the name stored in the container).")
    private Path output;

    @Override
    public Integer call() {
        try {
            Papyrus papyrus = PapyrusFile.read(file);
            byte[] data = papyrus.extract();
            Path destination = output != null ? output : defaultOutput(papyrus);
            Files.write(destination, data);
            System.out.println("[Extract] " + data.length + " bytes -> " + destination.toAbsolutePath());
            return 0;
        } catch (IOException e) {
            System.out.println("[Extract] Cannot read/write file: " + e.getMessage());
            return 1;
        } catch (IllegalStateException e) {
            System.out.println("[Extract] " + e.getMessage());
            return 1;
        }
    }

    private Path defaultOutput(Papyrus papyrus) {
        String name = papyrus.metadata.fileName != null ? papyrus.metadata.fileName : file.getFileName() + ".out";
        Path parent = file.toAbsolutePath().getParent();
        return parent != null ? parent.resolve(name) : Path.of(name);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Extract()).execute(args));
    }
}
