import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CodeWriter.java
 * Versión corregida y completa para Proyecto 8 (functions, call, return).
 *
 * - setFileName(filename) debe llamarse antes de traducir comandos de ese archivo (para naming static).
 * - writeInit() escribe bootstrap y llama a Sys.init.
 * - writePushPop: recibe "C_PUSH" o "C_POP" en el primer parámetro.
 */
public class CodeWriter {
    private final BufferedWriter out;
    private String fileName;           // nombre del archivo .vm actual (ej: Main.vm)
    private String currentFunction = "";
    private int labelCounter = 0;

    public CodeWriter(String outputFile) throws IOException {
        out = new BufferedWriter(new FileWriter(outputFile));
    }

    public void setFileName(String filename) {
        this.fileName = filename;
    }

    private String uniqueLabel(String base) {
        return base + "_" + (labelCounter++);
    }

    // ---------------- Bootstrap ----------------
    public void writeInit() throws IOException {
        out.write("// Bootstrap\n");
        out.write("@256\nD=A\n@SP\nM=D\n"); // SP = 256
        writeCall("Sys.init", 0);
    }


    // ---------------- Arithmetic ----------------
    public void writeArithmetic(String command) throws IOException {
        out.write("// " + command + "\n");
        switch (command) {
            case "add": binaryOp("M=M+D"); break;
            case "sub": binaryOp("M=M-D"); break;
            case "and": binaryOp("M=M&D"); break;
            case "or":  binaryOp("M=M|D"); break;
            case "neg": unaryOp("M=-M"); break;
            case "not": unaryOp("M=!M"); break;
            case "eq":  compareOp("JEQ"); break;
            case "gt":  compareOp("JGT"); break;
            case "lt":  compareOp("JLT"); break;
            default:
                throw new IllegalArgumentException("Unknown arithmetic command: " + command);
        }
    }

    private void binaryOp(String op) throws IOException {
        // pop y into D, apply to x at SP-1
        out.write("@SP\nAM=M-1\nD=M\nA=A-1\n" + op + "\n");
    }

    private void unaryOp(String op) throws IOException {
        out.write("@SP\nA=M-1\n" + op + "\n");
    }

    private void compareOp(String jmp) throws IOException {
        String t = uniqueLabel("TRUE");
        String e = uniqueLabel("END");
        out.write("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@" + t + "\nD;" + jmp + "\n");
        out.write("@SP\nA=M-1\nM=0\n@" + e + "\n0;JMP\n");
        out.write("(" + t + ")\n@SP\nA=M-1\nM=-1\n");
        out.write("(" + e + ")\n");
    }

    // ---------------- Push/Pop ----------------
    // command: "C_PUSH" or "C_POP"
    public void writePushPop(String command, String segment, int index) throws IOException {
        out.write("// " + command + " " + segment + " " + index + "\n");
        boolean isPush = "C_PUSH".equals(command);

        if (isPush) {
            switch (segment) {
                case "constant":
                    out.write("@" + index + "\nD=A\n");
                    pushD();
                    break;
                case "local":
                    pushFromSegment("LCL", index);
                    break;
                case "argument":
                    pushFromSegment("ARG", index);
                    break;
                case "this":
                    pushFromSegment("THIS", index);
                    break;
                case "that":
                    pushFromSegment("THAT", index);
                    break;
                case "temp":
                    out.write("@" + (5 + index) + "\nD=M\n");
                    pushD();
                    break;
                case "pointer":
                    out.write("@" + (index == 0 ? "THIS" : "THAT") + "\nD=M\n");
                    pushD();
                    break;
                case "static":
                    // static named by fileName without path
                    String baseStatic = fileName != null ? fileName.replace(".vm", "") : "Static";
                    out.write("@" + baseStatic + "." + index + "\nD=M\n");
                    pushD();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown push segment: " + segment);
            }
        } else { // pop
            switch (segment) {
                case "local":
                    popToSegment("LCL", index);
                    break;
                case "argument":
                    popToSegment("ARG", index);
                    break;
                case "this":
                    popToSegment("THIS", index);
                    break;
                case "that":
                    popToSegment("THAT", index);
                    break;
                case "temp":
                    out.write("@" + (5 + index) + "\nD=A\n@R13\nM=D\n");
                    popToR13();
                    break;
                case "pointer":
                    out.write("@SP\nAM=M-1\nD=M\n@" + (index == 0 ? "THIS" : "THAT") + "\nM=D\n");
                    break;
                case "static":
                    String baseStatic = fileName != null ? fileName.replace(".vm", "") : "Static";
                    out.write("@SP\nAM=M-1\nD=M\n@" + baseStatic + "." + index + "\nM=D\n");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown pop segment: " + segment);
            }
        }
    }

    private void pushD() throws IOException {
        out.write("@SP\nA=M\nM=D\n@SP\nM=M+1\n");
    }

    private void pushFromSegment(String base, int index) throws IOException {
        out.write("@" + index + "\nD=A\n@" + base + "\nA=M+D\nD=M\n");
        pushD();
    }

    private void popToSegment(String base, int index) throws IOException {
        // compute base+index into R13, then pop
        out.write("@" + index + "\nD=A\n@" + base + "\nD=M+D\n@R13\nM=D\n");
        popToR13();
    }

    private void popToR13() throws IOException {
        out.write("@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
    }

    // ---------------- Program flow ----------------
    public void writeLabel(String label) throws IOException {
        if (currentFunction == null || currentFunction.isEmpty()) {
            out.write("(" + label + ")\n");
        } else {
            out.write("(" + currentFunction + "$" + label + ")\n");
        }
    }

    public void writeGoto(String label) throws IOException {
        if (currentFunction == null || currentFunction.isEmpty()) {
            out.write("@" + label + "\n0;JMP\n");
        } else {
            out.write("@" + currentFunction + "$" + label + "\n0;JMP\n");
        }
    }

    public void writeIf(String label) throws IOException {
        if (currentFunction == null || currentFunction.isEmpty()) {
            out.write("@SP\nAM=M-1\nD=M\n@" + label + "\nD;JNE\n");
        } else {
            out.write("@SP\nAM=M-1\nD=M\n@" + currentFunction + "$" + label + "\nD;JNE\n");
        }
    }

    // ---------------- Function / Call / Return ----------------
    public void writeFunction(String functionName, int numLocals) throws IOException {
        currentFunction = functionName;
        out.write("// function " + functionName + " " + numLocals + "\n");
        out.write("(" + functionName + ")\n");
        // initialize local variables to 0
        for (int i = 0; i < numLocals; i++) {
            out.write("@SP\nA=M\nM=0\n@SP\nM=M+1\n");
        }
    }

    public void writeCall(String functionName, int numArgs) throws IOException {
        String returnLabel = uniqueLabel("RET_" + functionName);

        out.write("// call " + functionName + " " + numArgs + "\n");

        // push return-address
        out.write("@" + returnLabel + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");

        // push LCL, ARG, THIS, THAT
        pushSegmentValue("LCL");
        pushSegmentValue("ARG");
        pushSegmentValue("THIS");
        pushSegmentValue("THAT");

        // ARG = SP - numArgs - 5
        out.write("@SP\nD=M\n@" + (numArgs + 5) + "\nD=D-A\n@ARG\nM=D\n");

        // LCL = SP
        out.write("@SP\nD=M\n@LCL\nM=D\n");

        // goto functionName
        out.write("@" + functionName + "\n0;JMP\n");
        
        // (return-address)
        out.write("(" + returnLabel + ")\n");
    }


    private void pushSegmentValue(String seg) throws IOException {
        out.write("@" + seg + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
    }

    public void writeReturn() throws IOException {
        out.write("// return\n");
        // FRAME = LCL (usar R13 como FRAME)
        out.write("@LCL\nD=M\n@R13\nM=D\n");
        // RET = *(FRAME - 5) (usar R14 como RET)
        out.write("@5\nA=D-A\nD=M\n@R14\nM=D\n");
        // *ARG = pop() (coloca el valor de retorno para el caller)
        out.write("@SP\nAM=M-1\nD=M\n@ARG\nA=M\nM=D\n");
        // SP = ARG + 1
        out.write("@ARG\nD=M+1\n@SP\nM=D\n");
        // THAT = *(FRAME - 1)
        out.write("@R13\nAM=M-1\nD=M\n@THAT\nM=D\n");
        // THIS = *(FRAME - 2)
        out.write("@R13\nAM=M-1\nD=M\n@THIS\nM=D\n");
        // ARG = *(FRAME - 3)
        out.write("@R13\nAM=M-1\nD=M\n@ARG\nM=D\n");
        // LCL = *(FRAME - 4)
        out.write("@R13\nAM=M-1\nD=M\n@LCL\nM=D\n");
        // goto RET
        out.write("@R14\nA=M\n0;JMP\n");
    }

    // ---------------- Utilities ----------------
    public void close() throws IOException {
        // standard end loop (safe halt)
        out.write("(END)\n@END\n0;JMP\n");
        out.flush();
        out.close();
    }
}