package datachat.core;

import java.util.List;

public record QueryResult(List<String> columnNames, List<List<String>> rows) {
}
