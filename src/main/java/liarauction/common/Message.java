package liarauction.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 프로토콜 메시지 한 줄.
 *
 * <p>인자 안의 특수문자는 다음처럼 이스케이프해서 보낸다.
 * <ul>
 *   <li>{@code \} → {@code \\}</li>
 *   <li>{@code |} → {@code \p}</li>
 *   <li>줄바꿈 → {@code \n}, {@code \r}</li>
 * </ul>
 * 그래서 채팅에 {@code |}나 줄바꿈이 들어가도 메시지가 깨지지 않는다.
 */
public final class Message {

    private final String command;
    private final List<String> args;

    private Message(String command, List<String> args) {
        this.command = command;
        this.args = List.copyOf(args);
    }

    /** 메시지 생성. 인자는 {@code String.valueOf}로 문자열로 바꾼다. */
    public static Message of(String command, Object... args) {
        Objects.requireNonNull(command, "command");
        List<String> list = new ArrayList<>(args.length);
        for (Object arg : args) {
            list.add(String.valueOf(arg));
        }
        return new Message(command, list);
    }

    /**
     * 받은 한 줄을 메시지로 바꾼다.
     *
     * @throws IllegalArgumentException 빈 줄, 모르는 메시지, 인자 개수가 다를 때
     */
    public static Message parse(String line) {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("빈 메시지");
        }
        List<String> parts = split(line);
        String command = parts.get(0);
        List<String> args = parts.subList(1, parts.size());
        int expected = Protocol.argCount(command);
        if (expected < 0) {
            throw new IllegalArgumentException("모르는 메시지: " + command);
        }
        if (args.size() != expected) {
            throw new IllegalArgumentException(
                    command + " 인자 개수 " + expected + "개 필요, 받은 개수 " + args.size());
        }
        return new Message(command, args);
    }

    /** 보낼 한 줄 (줄바꿈 제외). */
    public String encode() {
        StringBuilder sb = new StringBuilder(escape(command));
        for (String arg : args) {
            sb.append(Protocol.DELIMITER).append(escape(arg));
        }
        return sb.toString();
    }

    public String command() {
        return command;
    }

    public List<String> args() {
        return args;
    }

    public String arg(int index) {
        return args.get(index);
    }

    public int intArg(int index) {
        try {
            return Integer.parseInt(args.get(index));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(command + " 인자 " + index + "가 숫자가 아님: " + args.get(index), e);
        }
    }

    public boolean boolArg(int index) {
        return Boolean.parseBoolean(args.get(index));
    }

    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '|' -> sb.append("\\p");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<String> split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == Protocol.DELIMITER) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else if (c == '\\') {
                if (i + 1 >= line.length()) {
                    throw new IllegalArgumentException("잘못된 이스케이프: " + line);
                }
                char next = line.charAt(++i);
                switch (next) {
                    case '\\' -> cur.append('\\');
                    case 'p' -> cur.append('|');
                    case 'n' -> cur.append('\n');
                    case 'r' -> cur.append('\r');
                    default -> throw new IllegalArgumentException("잘못된 이스케이프: \\" + next);
                }
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Message m && command.equals(m.command) && args.equals(m.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, args);
    }

    @Override
    public String toString() {
        return command + (args.isEmpty() ? "" : Arrays.toString(args.toArray()));
    }
}
