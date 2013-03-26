package pff;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 *  On each line, one entry consists of <i>field name</i> followed by value of that field without any quotes. <br>
 *  Any number of whitespaces allowed before <i>field name</i> and between <i>field name</i> and its value.<br>
 *  In value, newline characters should be encoded as \n
 *  and '\' characters should be escaped as "\\". <br>
 *  For checkboxes, values are 'Yes'/'Off'."<br>
 */
public class FormFieldsReader {
    private Scanner input;

    public FormFieldsReader(String source) {
        this.input = new Scanner(source);
    }

    public FormFieldsReader(Readable source) {
        input = new Scanner(source);
    }

    public Map<String, String> read() {
        Map<String, String> fields = new HashMap<String, String>();

        while(input.hasNext()) {
            String line = input.nextLine().trim();
            String[] tokens = split(line);
            if (tokens.length == 2) {
                fields.put(tokens[0], unescape(tokens[1]));
            }
        }

        return fields;
    }

    private String[] split(String line) {
        if (line.startsWith("[")) {
            String[] tokens = line.substring(1).split("]", 2);
            tokens[1] = tokens[1].trim();
            return tokens;
        } else
            return line.split("\\s", 2);
    }

    private String unescape(String str) {
        return str
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\p", "\u2029");
    }

}
