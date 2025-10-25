import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeWriter {
    private PrintWriter writer;
    private String fileName;
    private int labelCounter = 0;

    public CodeWriter(String outputFileName) throws IOException {
        writer = new PrintWriter(new FileWriter(outputFileName));
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void writeInit() {
        writer.println("// Bootstrap code");
        writer.println("@256");
        writer.println("D=A");
        writer.println("@SP");
        writer.println("M=D");
        writeCall("Sys.init", 0);
    }

    public void writeArithmetic(String command) {
        writer.println("// " + command);

        if (command.equals("add") || command.equals("sub") || command.equals("and") || command.equals("or")) {
            writer.println("@SP");
            writer.println("AM=M-1");
            writer.println("D=M");
            writer.println("A=A-1");

            switch (command) {
                case "add": writer.println("M=M+D"); break;
                case "sub": writer.println("M=M-D"); break;
                case "and": writer.println("M=M&D"); break;
                case "or":  writer.println("M=M|D"); break;
            }
        } 
        else if (command.equals("neg") || command.equals("not")) {
            writer.println("@SP");
            writer.println("A=M-1");
            writer.println(command.equals("neg") ? "M=-M" : "M=!M");
        } 
        else if (command.equals("eq") || command.equals("gt") || command.equals("lt")) {
            String trueLabel = "LABEL_TRUE" + labelCounter;
            String endLabel = "LABEL_END" + labelCounter;
            labelCounter++;

            writer.println("@SP");
            writer.println("AM=M-1");
            writer.println("D=M");
            writer.println("A=A-1");
            writer.println("D=M-D");
            writer.println("@" + trueLabel);
            switch (command) {
                case "eq": writer.println("D;JEQ"); break;
                case "gt": writer.println("D;JGT"); break;
                case "lt": writer.println("D;JLT"); break;
            }
            writer.println("@SP");
            writer.println("A=M-1");
            writer.println("M=0");
            writer.println("@" + endLabel);
            writer.println("0;JMP");
            writer.println("(" + trueLabel + ")");
            writer.println("@SP");
            writer.println("A=M-1");
            writer.println("M=-1");
            writer.println("(" + endLabel + ")");
        } 
        else if (command.equals("mul")) {
            // Nueva implementación: multiplicación por sumas sucesivas
            writer.println("// mul");
            writer.println("@SP");
            writer.println("AM=M-1");  // y
            writer.println("D=M");
            writer.println("@R13");    // guardar y
            writer.println("M=D");
            writer.println("@SP");
            writer.println("AM=M-1");  // x
            writer.println("D=M");
            writer.println("@R14");    // guardar x
            writer.println("M=D");
            writer.println("@0");
            writer.println("D=A");
            writer.println("@R15");    // acumulador = 0
            writer.println("M=D");
            String loop = "MUL_LOOP" + labelCounter;
            String end = "MUL_END" + labelCounter;
            labelCounter++;
            writer.println("(" + loop + ")");
            writer.println("@R13");
            writer.println("D=M");
            writer.println("@" + end);
            writer.println("D;JEQ");        // si y == 0 -> fin
            writer.println("@R15");
            writer.println("D=M");
            writer.println("@R14");
            writer.println("D=D+M");
            writer.println("@R15");
            writer.println("M=D");          // acumulador += x
            writer.println("@R13");
            writer.println("M=M-1");        // y--
            writer.println("@" + loop);
            writer.println("0;JMP");
            writer.println("(" + end + ")");
            writer.println("@R15");
            writer.println("D=M");
            writer.println("@SP");
            writer.println("A=M");
            writer.println("M=D");
            writer.println("@SP");
            writer.println("M=M+1");
        }
    }

    public void writePushPop(String command, String segment, int index) {
        writer.println("// " + command + " " + segment + " " + index);

        if (command.equals("C_PUSH")) {
            switch (segment) {
                case "constant":
                    writer.println("@" + index);
                    writer.println("D=A");
                    break;
                case "local":
                case "argument":
                case "this":
                case "that":
                    writer.println("@" + index);
                    writer.println("D=A");
                    writer.println("@" + segmentToPointer(segment));
                    writer.println("A=M+D");
                    writer.println("D=M");
                    break;
                case "temp":
                    writer.println("@" + (5 + index));
                    writer.println("D=M");
                    break;
                case "pointer":
                    writer.println("@" + (index == 0 ? "THIS" : "THAT"));
                    writer.println("D=M");
                    break;
                case "static":
                    writer.println("@" + fileName + "." + index);
                    writer.println("D=M");
                    break;
            }
            writer.println("@SP");
            writer.println("A=M");
            writer.println("M=D");
            writer.println("@SP");
            writer.println("M=M+1");
        } 
        else if (command.equals("C_POP")) {
            switch (segment) {
                case "local":
                case "argument":
                case "this":
                case "that":
                    writer.println("@" + index);
                    writer.println("D=A");
                    writer.println("@" + segmentToPointer(segment));
                    writer.println("D=M+D");
                    writer.println("@R13");
                    writer.println("M=D");
                    break;
                case "temp":
                    writer.println("@" + (5 + index));
                    writer.println("D=A");
                    writer.println("@R13");
                    writer.println("M=D");
                    break;
                case "pointer":
                    writer.println("@" + (index == 0 ? "THIS" : "THAT"));
                    writer.println("D=A");
                    writer.println("@R13");
                    writer.println("M=D");
                    break;
                case "static":
                    writer.println("@" + fileName + "." + index);
                    writer.println("D=A");
                    writer.println("@R13");
                    writer.println("M=D");
                    break;
            }
            writer.println("@SP");
            writer.println("AM=M-1");
            writer.println("D=M");
            writer.println("@R13");
            writer.println("A=M");
            writer.println("M=D");
        }
    }

    private String segmentToPointer(String segment) {
        switch (segment) {
            case "local": return "LCL";
            case "argument": return "ARG";
            case "this": return "THIS";
            case "that": return "THAT";
        }
        return "";
    }

    public void writeLabel(String label) {
        writer.println("(" + fileName.replace(".vm", "") + "$" + label + ")");
    }

    public void writeGoto(String label) {
        writer.println("@" + fileName.replace(".vm", "") + "$" + label);
        writer.println("0;JMP");
    }

    public void writeIf(String label) {
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@" + fileName.replace(".vm", "") + "$" + label);
        writer.println("D;JNE");
    }

    public void writeCall(String functionName, int numArgs) {
        String returnLabel = "RET_" + functionName + labelCounter++;
        writer.println("// call " + functionName + " " + numArgs);
        writer.println("@" + returnLabel);
        writer.println("D=A");
        writer.println("@SP");
        writer.println("A=M");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("M=M+1");

        for (String seg : new String[]{"LCL", "ARG", "THIS", "THAT"}) {
            writer.println("@" + seg);
            writer.println("D=M");
            writer.println("@SP");
            writer.println("A=M");
            writer.println("M=D");
            writer.println("@SP");
            writer.println("M=M+1");
        }

        writer.println("@SP");
        writer.println("D=M");
        writer.println("@" + (5 + numArgs));
        writer.println("D=D-A");
        writer.println("@ARG");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("D=M");
        writer.println("@LCL");
        writer.println("M=D");
        writer.println("@" + functionName);
        writer.println("0;JMP");
        writer.println("(" + returnLabel + ")");
    }

    public void writeReturn() {
        writer.println("// return");
        writer.println("@LCL");
        writer.println("D=M");
        writer.println("@R13");
        writer.println("M=D");
        writer.println("@5");
        writer.println("A=D-A");
        writer.println("D=M");
        writer.println("@R14");
        writer.println("M=D");
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@ARG");
        writer.println("A=M");
        writer.println("M=D");
        writer.println("@ARG");
        writer.println("D=M+1");
        writer.println("@SP");
        writer.println("M=D");

        String[] segs = {"THAT", "THIS", "ARG", "LCL"};
        for (int i = 0; i < segs.length; i++) {
            writer.println("@R13");
            writer.println("D=M");
            writer.println("@" + (i + 1));
            writer.println("A=D-A");
            writer.println("D=M");
            writer.println("@" + segs[i]);
            writer.println("M=D");
        }

        writer.println("@R14");
        writer.println("A=M");
        writer.println("0;JMP");
    }

    public void writeFunction(String functionName, int numLocals) {
        writer.println("// function " + functionName + " " + numLocals);
        writer.println("(" + functionName + ")");
        for (int i = 0; i < numLocals; i++) {
            writer.println("@0");
            writer.println("D=A");
            writer.println("@SP");
            writer.println("A=M");
            writer.println("M=D");
            writer.println("@SP");
            writer.println("M=M+1");
        }
    }

    public void close() {
        writer.close();
    }
}
