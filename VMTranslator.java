import java.io.*;

public class VMTranslator {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Uso: java VMTranslator archivo.vm");
            return;
        }

        String inputFile = args[0];
        String outputFile = inputFile.replace(".vm", ".asm");

        Parser parser = new Parser(inputFile);
        CodeWriter codeWriter = new CodeWriter(outputFile);

        while (parser.hasMoreCommands()) {
            parser.advance();
            String commandType = parser.commandType();

            switch (commandType) {
                case "C_ARITHMETIC":
                    codeWriter.writeArithmetic(parser.arg1());
                    break;
                case "C_PUSH":
                case "C_POP":
                    codeWriter.writePushPop(commandType, parser.arg1(), parser.arg2());
                    break;
                case "C_LABEL":
                    codeWriter.writeLabel(parser.arg1());
                    break;
                case "C_GOTO":
                    codeWriter.writeGoto(parser.arg1());
                    break;
                case "C_IF":
                    codeWriter.writeIf(parser.arg1());
                    break;
                case "C_FUNCTION":
                    codeWriter.writeFunction(parser.arg1(), Integer.parseInt(parser.arg2()));
                    break;
                case "C_CALL":
                    codeWriter.writeCall(parser.arg1(), Integer.parseInt(parser.arg2()));
                    break;
                case "C_RETURN":
                    codeWriter.writeReturn();
                    break;
            }
        }

        codeWriter.close();
    }
}
