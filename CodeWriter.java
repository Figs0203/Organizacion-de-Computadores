import java.io.*;

public class CodeWriter {
    private PrintWriter writer;
    private String fileName;
    private int labelCounter = 0;
    private int returnCounter = 0;

    public CodeWriter(String outputFile) throws IOException {
        writer = new PrintWriter(new FileWriter(outputFile));
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void close() {
        writer.close();
    }

    // ==========================
    //   BOOTSTRAP (SP = 256)
    // ==========================
    public void writeInit() {
        writer.println("// Bootstrap code");
        writer.println("@256");
        writer.println("D=A");
        writer.println("@SP");
        writer.println("M=D");
        writeCall("Sys.init", 0);
    }

    // ==========================
    //   ARITHMETIC COMMANDS
    // ==========================
    public void writeArithmetic(String command) {
        writer.println("// " + command);
        switch (command) {
            case "add":
            case "sub":
            case "and":
            case "or":
                binaryOp(command);
                break;
            case "neg":
            case "not":
                unaryOp(command);
                break;
            case "eq":
            case "gt":
            case "lt":
                compareOp(command);
                break;
        }
    }

    private void binaryOp(String cmd) {
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("A=A-1");
        switch (cmd) {
            case "add": writer.println("M=D+M"); break;
            case "sub": writer.println("M=M-D"); break;
            case "and": writer.println("M=D&M"); break;
            case "or":  writer.println("M=D|M"); break;
        }
    }

    private void unaryOp(String cmd) {
        writer.println("@SP");
        writer.println("A=M-1");
        if (cmd.equals("neg")) writer.println("M=-M");
        else writer.println("M=!M");
    }

    private void compareOp(String cmd) {
        String labelTrue = "LABEL_TRUE" + labelCounter;
        String labelEnd = "LABEL_END" + labelCounter;
        labelCounter++;

        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("A=A-1");
        writer.println("D=M-D");
        writer.println("@" + labelTrue);

        switch (cmd) {
            case "eq": writer.println("D;JEQ"); break;
            case "gt": writer.println("D;JGT"); break;
            case "lt": writer.println("D;JLT"); break;
        }

        writer.println("@SP");
        writer.println("A=M-1");
        writer.println("M=0");
        writer.println("@" + labelEnd);
        writer.println("0;JMP");
        writer.println("(" + labelTrue + ")");
        writer.println("@SP");
        writer.println("A=M-1");
        writer.println("M=-1");
        writer.println("(" + labelEnd + ")");
    }

    // ==========================
    //   PUSH / POP
    // ==========================
    public void writePushPop(String commandType, String segment, int index) {
        writer.println("// " + commandType + " " + segment + " " + index);

        if (commandType.equals("C_PUSH")) {
            if (segment.equals("constant")) {
                writer.println("@" + index);
                writer.println("D=A");
            } else if (segment.equals("local") || segment.equals("argument") ||
                       segment.equals("this") || segment.equals("that")) {
                String base = getBase(segment);
                writer.println("@" + index);
                writer.println("D=A");
                writer.println("@" + base);
                writer.println("A=D+M");
                writer.println("D=M");
            } else if (segment.equals("temp")) {
                writer.println("@" + (5 + index));
                writer.println("D=M");
            } else if (segment.equals("pointer")) {
                writer.println("@" + (index == 0 ? "THIS" : "THAT"));
                writer.println("D=M");
            } else if (segment.equals("static")) {
                writer.println("@" + fileName + "." + index);
                writer.println("D=M");
            }
            pushD();

        } else if (commandType.equals("C_POP")) {
            if (segment.equals("local") || segment.equals("argument") ||
                segment.equals("this") || segment.equals("that")) {
                String base = getBase(segment);
                writer.println("@" + index);
                writer.println("D=A");
                writer.println("@" + base);
                writer.println("D=D+M");
                writer.println("@R13");
                writer.println("M=D");
                popToD();
                writer.println("@R13");
                writer.println("A=M");
                writer.println("M=D");
            } else if (segment.equals("temp")) {
                popToD();
                writer.println("@" + (5 + index));
                writer.println("M=D");
            } else if (segment.equals("pointer")) {
                popToD();
                writer.println("@" + (index == 0 ? "THIS" : "THAT"));
                writer.println("M=D");
            } else if (segment.equals("static")) {
                popToD();
                writer.println("@" + fileName + "." + index);
                writer.println("M=D");
            }
        }
    }

    private void pushD() {
        writer.println("@SP");
        writer.println("A=M");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("M=M+1");
    }

    private void popToD() {
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
    }

    private String getBase(String segment) {
        switch (segment) {
            case "local": return "LCL";
            case "argument": return "ARG";
            case "this": return "THIS";
            case "that": return "THAT";
            default: return "";
        }
    }

    // ==========================
    //   CONTROL FLOW
    // ==========================
    public void writeLabel(String label) {
        writer.println("(" + fileName + "$" + label + ")");
    }

    public void writeGoto(String label) {
        writer.println("@" + fileName + "$" + label);
        writer.println("0;JMP");
    }

    public void writeIf(String label) {
        popToD();
        writer.println("@" + fileName + "$" + label);
        writer.println("D;JNE");
    }

    // ==========================
    //   FUNCTION CALLS
    // ==========================
    public void writeCall(String functionName, int numArgs) {
        String returnLabel = "RET_" + functionName + returnCounter++;
        writer.println("// call " + functionName + " " + numArgs);

        // push return address
        writer.println("@" + returnLabel);
        writer.println("D=A");
        pushD();

        // push LCL, ARG, THIS, THAT
        for (String seg : new String[]{"LCL", "ARG", "THIS", "THAT"}) {
            writer.println("@" + seg);
            writer.println("D=M");
            pushD();
        }

        // ARG = SP - nArgs - 5
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@" + (numArgs + 5));
        writer.println("D=D-A");
        writer.println("@ARG");
        writer.println("M=D");

        // LCL = SP
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@LCL");
        writer.println("M=D");

        // goto function
        writer.println("@" + functionName);
        writer.println("0;JMP");

        // return address label
        writer.println("(" + returnLabel + ")");
    }

    public void writeReturn() {
        writer.println("// return");
        // FRAME = LCL
        writer.println("@LCL");
        writer.println("D=M");
        writer.println("@R13");
        writer.println("M=D");

        // RET = *(FRAME-5)
        writer.println("@5");
        writer.println("A=D-A");
        writer.println("D=M");
        writer.println("@R14");
        writer.println("M=D");

        // *ARG = pop()
        popToD();
        writer.println("@ARG");
        writer.println("A=M");
        writer.println("M=D");

        // SP = ARG + 1
        writer.println("@ARG");
        writer.println("D=M+1");
        writer.println("@SP");
        writer.println("M=D");

        // restore THAT, THIS, ARG, LCL
        restoreSegment("THAT", 1);
        restoreSegment("THIS", 2);
        restoreSegment("ARG", 3);
        restoreSegment("LCL", 4);

        // goto RET
        writer.println("@R14");
        writer.println("A=M");
        writer.println("0;JMP");
    }

    private void restoreSegment(String segment, int offset) {
        writer.println("@R13");
        writer.println("D=M");
        writer.println("@" + offset);
        writer.println("A=D-A");
        writer.println("D=M");
        writer.println("@" + segment);
        writer.println("M=D");
    }

    public void writeFunction(String functionName, int nLocals) {
        writer.println("// function " + functionName + " " + nLocals);
        writer.println("(" + functionName + ")");
        for (int i = 0; i < nLocals; i++) {
            writer.println("@0");
            writer.println("D=A");
            pushD();
        }
    }
}
