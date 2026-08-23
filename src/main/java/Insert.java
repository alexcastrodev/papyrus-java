import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "insert", mixinStandardHelpOptions = true,
        description = "Inserts a fragment file into a Papyrus base. Ignores it if the page is already present.")
class Insert implements Callable<Integer> {

    @Parameters(index = "0", description = ".papyrus file to update.")
    private Path papyrusFile;

    @Parameters(index = "1", description = ".frag fragment file to insert.")
    private Path fragmentFile;

    @Override
    public Integer call() {
        try {
            Papyrus papyrus = PapyrusFile.read(papyrusFile);
            Leaf leaf = FragmentFile.read(fragmentFile);

            if (papyrus.fragments.containsKey(leaf.page)) {
                System.out.println("[Insert] page " + leaf.page + " already present, ignoring");
                return 0;
            }

            papyrus.addFragment(leaf);
            PapyrusFile.write(papyrus, papyrusFile);
            System.out.println("[Insert] page " + leaf.page + " inserted, status=" + papyrus.status
                    + " -> " + papyrusFile.toAbsolutePath());
            return 0;
        } catch (IOException e) {
            System.out.println("[Insert] Cannot read/write file: " + e.getMessage());
            return 1;
        } catch (IllegalArgumentException e) {
            System.out.println("[Insert] " + e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Insert()).execute(args));
    }
}
