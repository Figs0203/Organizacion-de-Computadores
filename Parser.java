import java.io.*;
import java.util.*;

public class Parser {
    private List<String> lines;
    private int currentCommand;

    public Parser(String inputFile) throws IOException {
        lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.split("//")[0].trim(); // elimina comentarios
            if (!line.equals("")) {
                lines.add(line);
            }
        }
        br.close();
        currentCommand = -1;
    }

    public boolean hasMoreCommands() {
        return currentCommand < lines.size() - 1;
    }

    public void advance() {
        currentCommand++;
    }

    private String[] parts() {
        return lines.get(currentCommand).split(" ");
    }

    public String commandType() {
        String cmd = parts()[0];
        switch (cmd) {
            case "push":
                return "C_PUSH";
            case "pop":
                return "C_POP";
            case "label":
                return "C_LABEL";
            case "goto":
                return "C_GOTO";
            case "if-goto":
                return "C_IF";
            case "function":
                return "C_FUNCTION";
            case "call":
                return "C_CALL";
            case "return":
                return "C_RETURN";
            default:
                return "C_ARITHMETIC";
        }
    }

    public String arg1() {
        if (commandType().equals("C_ARITHMETIC")) return parts()[0];
        return parts()[1];
    }

    public String arg2() {
        if (parts().length > 2) return parts()[2];
        return null;
    }
}
