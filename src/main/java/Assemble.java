import java.io.FileInputStream;
import java.io.IOException;

class Assemble {
    void main(String[] args) {
        try (FileInputStream data = new FileInputStream("book.pdf")) {
            byte[] block = data.readNBytes(4);
            String fingerprint = "";

            // TODO: gerar fingerprint do bloco e montar o Leaf
            // new Leaf(page, totalPages, rootFingerprint, merkleProof, block);
        } catch (IOException e) {
            System.out.println("[Assemble] Cannot read file: " + e.getMessage());
        }
    }
}

