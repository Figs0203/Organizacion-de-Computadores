import java.io.*;

public class CodeWriter {
    private BufferedWriter writer;

    public CodeWriter(String outputFile) throws IOException {
        writer = new BufferedWriter(new FileWriter(outputFile));
    }

    public void writeArithmetic(String command) throws IOException {
        writer.write("// " + command + "\n");
        // Aquí agregarás la traducción de comandos aritméticos (add, sub, etc.)
    }

    public void writePushPop(String commandType, String segment, String index) throws IOException {
        writer.write("// " + commandType + " " + segment + " " + index + "\n");
        // Aquí colocarás la lógica de push/pop
    }

    public void writeLabel(String label) throws IOException {
        writer.write("// label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        writer.write("// goto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        writer.write("// if-goto " + label + "\n");
    }

    public void writeFunction(String functionName, int numLocals) throws IOException {
        writer.write("// function " + functionName + " " + numLocals + "\n");
    }

    public void writeCall(String functionName, int numArgs) throws IOException {
        writer.write("// call " + functionName + " " + numArgs + "\n");
    }

    public void writeReturn() throws IOException {
        writer.write("// return\n");
    }

    public void close() throws IOException {
        writer.close();
    }
}
