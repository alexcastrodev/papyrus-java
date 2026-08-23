# Papyrus Java implementation

Essa implementação é um teste de uma tese: https://github.com/castro-research/papyrus-protocol

# Build

mvn package -DskipTests

# Create a simple file

```bash
echo "This is a looooooooooong text splited by 24 bytes" >> example.txt 
```

# Gerar o papyrus e seus fragmentos

java -jar target/papyrus-protocol-0.1.0-SNAPSHOT.jar example.txt -p 24

# Insert fragment into papyrus

```bash
for f in out/example.txt.*.frag; do
    java -cp target/papyrus-protocol-0.1.0-SNAPSHOT.jar Insert example.txt.papyrus "$f"
done
```

# Extract (rebuild) the original file

```bash
java -cp target/papyrus-protocol-0.1.0-SNAPSHOT.jar Extract example.txt.papyrus -o example.rebuilt.txt
```
