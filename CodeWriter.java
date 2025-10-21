import java.io.*;

public class CodeWriter {
    private BufferedWriter writer;
    private int labelCount = 0;

    public CodeWriter(String outputFile) throws IOException {
        writer = new BufferedWriter(new FileWriter(outputFile));
    }

    public void writeArithmetic(String command) throws IOException {
        writer.write("// " + command + "\n");
        switch (command) {
            case "add":
                writer.write("@SP\nAM=M-1\nD=M\nA=A-1\nM=M+D\n");
                break;
            case "sub":
                writer.write("@SP\nAM=M-1\nD=M\nA=A-1\nM=M-D\n");
                break;
            case "neg":
                writer.write("@SP\nA=M-1\nM=-M\n");
                break;
            case "eq":
            case "gt":
            case "lt":
                String labelTrue = "LABEL_TRUE" + labelCount;
                String labelEnd = "LABEL_END" + labelCount;
                labelCount++;
                writer.write("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n");
                if (command.equals("eq")) {
                    writer.write("@" + labelTrue + "\nD;JEQ\n");
                } else if (command.equals("gt")) {
                    writer.write("@" + labelTrue + "\nD;JGT\n");
                } else if (command.equals("lt")) {
                    writer.write("@" + labelTrue + "\nD;JLT\n");
                }
                writer.write("@SP\nA=M-1\nM=0\n@" + labelEnd + "\n0;JMP\n");
                writer.write("(" + labelTrue + ")\n@SP\nA=M-1\nM=-1\n");
                writer.write("(" + labelEnd + ")\n");
                break;
            case "and":
                writer.write("@SP\nAM=M-1\nD=M\nA=A-1\nM=M&D\n");
                break;
            case "or":
                writer.write("@SP\nAM=M-1\nD=M\nA=A-1\nM=M|D\n");
                break;
            case "not":
                writer.write("@SP\nA=M-1\nM=!M\n");
                break;
        }
    }

    public void writePushPop(String commandType, String segment, String index) throws IOException {
        writer.write("// " + commandType + " " + segment + " " + index + "\n");
        if (commandType.equals("C_PUSH")) {
            if (segment.equals("constant")) {
                writer.write("@" + index + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("local")) {
                writer.write("@" + index + "\nD=A\n@LCL\nA=M+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("argument")) {
                writer.write("@" + index + "\nD=A\n@ARG\nA=M+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("this")) {
                writer.write("@" + index + "\nD=A\n@THIS\nA=M+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("that")) {
                writer.write("@" + index + "\nD=A\n@THAT\nA=M+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("pointer")) {
                writer.write("@" + (index.equals("0") ? "THIS" : "THAT") + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("temp")) {
                writer.write("@R" + (Integer.parseInt(index) + 5) + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            } else if (segment.equals("static")) {
                writer.write("@Static." + index + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
            }
        } else if (commandType.equals("C_POP")) {
            if (segment.equals("local")) {
                writer.write("@" + index + "\nD=A\n@LCL\nD=M+D\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("argument")) {
                writer.write("@" + index + "\nD=A\n@ARG\nD=M+D\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("this")) {
                writer.write("@" + index + "\nD=A\n@THIS\nD=M+D\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("that")) {
                writer.write("@" + index + "\nD=A\n@THAT\nD=M+D\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
            } else if (segment.equals("pointer")) {
                writer.write("@SP\nAM=M-1\nD=M\n@" + (index.equals("0") ? "THIS" : "THAT") + "\nM=D\n");
            } else if (segment.equals("temp")) {
                writer.write("@SP\nAM=M-1\nD=M\n@R" + (Integer.parseInt(index) + 5) + "\nM=D\n");
            } else if (segment.equals("static")) {
                writer.write("@SP\nAM=M-1\nD=M\n@Static." + index + "\nM=D\n");
            }
        }
    }

    // Estas funciones las puedes ir agregando para trabajar con funciones VM, flujo y más:
    public void writeLabel(String label) throws IOException { /* implementar si se requiere */}
    public void writeGoto(String label) throws IOException { /* implementar si se requiere */}
    public void writeIf(String label) throws IOException { /* implementar si se requiere */}
    public void writeFunction(String functionName, int numLocals) throws IOException { /* implementar */}
    public void writeCall(String functionName, int numArgs) throws IOException { /* implementar */}
    public void writeReturn() throws IOException { /* implementar */}

    public void close() throws IOException {
        writer.close();
    }
}
