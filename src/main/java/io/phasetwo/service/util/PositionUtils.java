package io.phasetwo.service.util;

import io.phasetwo.service.model.OrganizationPositionModel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class PositionUtils {

  public static Stream<OrganizationPositionModel> expandPosition(OrganizationPositionModel position) {
    return expandPositionsStream(Stream.of(position));
  }

  public static Stream<OrganizationPositionModel> expandPositionsStream(Stream<OrganizationPositionModel> positions) {
    Set<String> visited = new HashSet<>();
    return positions.flatMap(it -> expandPositionsStream(it, visited));
  }

  private static Stream<OrganizationPositionModel> expandPositionsStream(
      OrganizationPositionModel position,
      Set<String> visitedIds
  ) {
    Stream.Builder<OrganizationPositionModel> sb = Stream.builder();

    if (!visitedIds.contains(position.getId())) {
      Deque<OrganizationPositionModel> stack = new ArrayDeque<>();
      stack.add(position);

      while (!stack.isEmpty()) {
        OrganizationPositionModel current = stack.pop();
        sb.add(current);

        current.getSubordinateStream()
            .filter(it -> !visitedIds.contains(it.getId()))
            .forEach(it -> {
              visitedIds.add(it.getId());
              stack.add(it);
            });
      }
    }

    return sb.build();
  }
}
