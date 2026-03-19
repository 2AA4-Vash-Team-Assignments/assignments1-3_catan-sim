package ca.mcmaster.se2aa4.catan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern ROLL = Pattern.compile("^\\s*(?i)roll\\s*$");
    private static final Pattern GO = Pattern.compile("^\\s*(?i)go\\s*$");
    private static final Pattern LIST = Pattern.compile("^\\s*(?i)list\\s*$");
    private static final Pattern UNDO = Pattern.compile("^\\s*(?i)undo\\s*$");
    private static final Pattern REDO = Pattern.compile("^\\s*(?i)redo\\s*$");
    private static final Pattern BUILD_SETTLEMENT = Pattern.compile("^\\s*(?i)build\\s+settlement\\s+(\\d+)\\s*$");
    private static final Pattern BUILD_CITY = Pattern.compile("^\\s*(?i)build\\s+city\\s+(\\d+)\\s*$");
    private static final Pattern BUILD_ROAD = Pattern.compile("^\\s*(?i)build\\s+road\\s+(\\d+)\\s*,\\s*(\\d+)\\s*$");

    public ParsedCommand parse(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedCommand(CommandType.UNKNOWN);
        }
        String trimmed = input.trim();

        if (ROLL.matcher(trimmed).matches()) {
            return new ParsedCommand(CommandType.ROLL);
        }
        if (GO.matcher(trimmed).matches()) {
            return new ParsedCommand(CommandType.GO);
        }
        if (LIST.matcher(trimmed).matches()) {
            return new ParsedCommand(CommandType.LIST);
        }
        if (UNDO.matcher(trimmed).matches()) {
            return new ParsedCommand(CommandType.UNDO);
        }
        if (REDO.matcher(trimmed).matches()) {
            return new ParsedCommand(CommandType.REDO);
        }

        Matcher settlementMatcher = BUILD_SETTLEMENT.matcher(trimmed);
        if (settlementMatcher.matches()) {
            int nodeId = Integer.parseInt(settlementMatcher.group(1));
            return new ParsedCommand(CommandType.BUILD_SETTLEMENT, nodeId);
        }

        Matcher cityMatcher = BUILD_CITY.matcher(trimmed);
        if (cityMatcher.matches()) {
            int nodeId = Integer.parseInt(cityMatcher.group(1));
            return new ParsedCommand(CommandType.BUILD_CITY, nodeId);
        }

        Matcher roadMatcher = BUILD_ROAD.matcher(trimmed);
        if (roadMatcher.matches()) {
            int from = Integer.parseInt(roadMatcher.group(1));
            int to = Integer.parseInt(roadMatcher.group(2));
            return new ParsedCommand(CommandType.BUILD_ROAD, -1, from, to);
        }

        return new ParsedCommand(CommandType.UNKNOWN);
    }
}
