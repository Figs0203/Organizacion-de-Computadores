import java.io.*;
import java.util.*;

public class VMTranslator {
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Uso: VMTranslator <archivo o carpeta>");
            return;
        }

        File input = new File(args[0]);
        List<File> vmFiles = new ArrayList<>();
        String outputFile;

        if (input.isDirectory()) {
            // Si se pasa una carpeta, traducir todos los archivos .vm dentro
            File[] files = input.listFiles((dir, name) -> name.endsWith(".vm"));
            if (files != null) vmFiles.addAll(Arrays.asList(files));
            outputFile = input.getAbsolutePath() + "/" + input.getName() + ".asm";
        } else {
            // Si se pasa un solo archivo .vm
            vmFiles.add(input);
            outputFile = input.getAbsolutePath().replace(".vm", ".asm");
        }

        CodeWriter codeWriter = new CodeWriter(outputFile);

        // Bootstrap: inicializa SP = 256 y llama a Sys.init
        codeWriter.writeInit();

        // Traducir cada archivo .vm
        for (File vmFile : vmFiles) {
            Parser parser = new Parser(vmFile.getAbsolutePath());
            codeWriter.setFileName(vmFile.getName().replace(".vm", ""));

            while (parser.hasMoreCommands()) {
                parser.advance();
                String commandType = parser.commandType();

                switch (commandType) {
                    case "C_ARITHMETIC":
                        codeWriter.writeArithmetic(parser.arg1());
                        break;

                    case "C_PUSH":
                    case "C_POP":
                        codeWriter.writePushPop(commandType, parser.arg1(), Integer.parseInt(parser.arg2()));
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
        }

        codeWriter.close();
        System.out.println("✅ Traducción completada: " + outputFile);
    }
}
