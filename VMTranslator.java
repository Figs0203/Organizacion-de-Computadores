import java.io.File;
import java.io.IOException;

public class VMTranslator {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Uso: VMTranslator <archivo o carpeta>");
            return;
        }

        File input = new File(args[0]);

        // Si es un archivo único .vm
        if (input.isFile() && input.getName().endsWith(".vm")) {
            String outputFileName = input.getAbsolutePath().replace(".vm", ".asm");
            Parser parser = new Parser(input.getAbsolutePath());
            CodeWriter codeWriter = new CodeWriter(outputFileName);
            codeWriter.setFileName(input.getName());

            while (parser.hasMoreCommands()) {
                parser.advance();
                processCommand(parser, codeWriter);
            }

            codeWriter.close();
            System.out.println("Archivo traducido: " + outputFileName);
            return;
        }

        // Si es una carpeta que contiene varios .vm
        if (input.isDirectory()) {
            File[] vmFiles = input.listFiles((dir, name) -> name.endsWith(".vm"));
            if (vmFiles == null || vmFiles.length == 0) {
                System.out.println("No se encontraron archivos .vm en la carpeta.");
                return;
            }

            String outputFileName = input.getAbsolutePath() + File.separator + input.getName() + ".asm";
            CodeWriter codeWriter = new CodeWriter(outputFileName);

            // Bootstrap
            codeWriter.writeInit();

            // Procesar todos los .vm en la carpeta
            for (File vmFile : vmFiles) {
                Parser parser = new Parser(vmFile.getAbsolutePath());
                codeWriter.setFileName(vmFile.getName());

                while (parser.hasMoreCommands()) {
                    parser.advance();
                    processCommand(parser, codeWriter);
                }
            }

            codeWriter.close();
            System.out.println("Traducción completa: " + outputFileName);
            return;
        }

        System.out.println("Error: Debe especificar un archivo .vm o una carpeta que contenga archivos .vm");
    }

    private static void processCommand(Parser parser, CodeWriter codeWriter) throws IOException {
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
            default:
                // Ignorar líneas vacías o comentarios
                break;
        }
    }
}
