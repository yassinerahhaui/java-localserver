package config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class SimpleJsonParser {
    private final String json;
    private int pos;

    public SimpleJsonParser(String json) {
        this.json = json != null ? json : "";
        this.pos = 0;
    }

    public static SimpleJsonParser fromFile(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return new SimpleJsonParser(sb.toString());
    }

    public Object parse() {
        skipWhitespace();
        Object result = parseValue();
        skipWhitespace();
        return result;
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length()) {
            return null;
        }

        char c = json.charAt(pos);
        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"') {
            return parseString();
        } else if (c == 't' || c == 'f') {
            return parseBoolean();
        } else if (c == 'n') {
            return parseNull();
        } else if (c == '-' || Character.isDigit(c)) {
            return parseNumber();
        }

        throw new RuntimeException("Syntax Error f JSON 3nd position " + pos + ": 7arf ghrib '" + c + "'");
    }

    public Map<String, Object> parseObject() {
        match('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();

        if (peek() == '}') {
            match('}');
            return map;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            match(':');
            Object value = parseValue();
            map.put(key, value);

            skipWhitespace();
            char next = peek();
            if (next == '}') {
                match('}');
                break;
            } else if (next == ',') {
                match(',');
            } else {
                throw new RuntimeException("Expected ',' wla '}' 3nd position " + pos);
            }
        }
        return map;
    }

    public List<Object> parseArray() {
        match('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();

        if (peek() == ']') {
            match(']');
            return list;
        }

        while (true) {
            list.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ']') {
                match(']');
                break;
            } else if (next == ',') {
                match(',');
            } else {
                throw new RuntimeException("Expected ',' wla ']' 3nd position " + pos);
            }
        }
        return list;
    }

    private String parseString() {
        match('"');
        StringBuilder sb = new StringBuilder();

        while (pos < json.length()) {
            char c = json.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\' && pos < json.length()) {
                char esc = json.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }
        throw new RuntimeException("Unterminated string f JSON");
    }

    private Number parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < json.length() && Character.isDigit(peek())) pos++;

        boolean isDecimal = false;
        if (pos < json.length() && peek() == '.') {
            isDecimal = true;
            pos++;
            while (pos < json.length() && Character.isDigit(peek())) pos++;
        }

        String numStr = json.substring(start, pos);
        if (isDecimal) {
            return Double.parseDouble(numStr);
        } else {
            return Long.parseLong(numStr);
        }
    }

    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new RuntimeException("Expected boolean 3nd position " + pos);
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new RuntimeException("Expected null 3nd position " + pos);
    }

    private void skipWhitespace() {
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private char peek() {
        if (pos >= json.length()) return '\0';
        return json.charAt(pos);
    }

    private void match(char expected) {
        if (pos >= json.length() || json.charAt(pos) != expected) {
            throw new RuntimeException("Expected '" + expected + "' walakin lqina '" + peek() + "' 3nd pos " + pos);
        }
        pos++;
    }

   
    public static long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 0;
        }

        sizeStr = sizeStr.trim().toUpperCase();
        long multiplier = 1;

        if (sizeStr.endsWith("GB") || sizeStr.endsWith("G")) {
            multiplier = 1024L * 1024L * 1024L;
            sizeStr = sizeStr.replaceAll("[^0-9]", "");
        } else if (sizeStr.endsWith("MB") || sizeStr.endsWith("M")) {
            multiplier = 1024L * 1024L;
            sizeStr = sizeStr.replaceAll("[^0-9]", "");
        } else if (sizeStr.endsWith("KB") || sizeStr.endsWith("K")) {
            multiplier = 1024L;
            sizeStr = sizeStr.replaceAll("[^0-9]", "");
        } else if (sizeStr.endsWith("B")) {
            sizeStr = sizeStr.replaceAll("[^0-9]", "");
        }

        try {
            return Long.parseLong(sizeStr.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    
}
