package dev.coms4156.project.livesched;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Represents a task that has to be done. 
 * This class stores the id, resources needed, and the location of the task.
 */
public class Task implements Serializable {
  @Serial
  private static final long serialVersionUID = 1001L;

  private final String taskId;
  private Map<ResourceType, Integer> resourceList; // Key = ResourceType, Value = Units needed
  private int priority; // value between 1 and 5
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Location location;

  /**
   * Constructs a new Task object with the given parameters.
   *
   * @param taskId        the unique ID of the resource
   * @param resourceList  the map of ResourceTypes and their quantity needed
   * @param priority      the priority of the task
   * @param startTime     the time that task starts
   * @param endTime       the time that task ends
   * @param latitude      the latitude of the task's location
   * @param longitude     the longitude of the task's location
   * @throws IllegalArgumentException if the {@code taskId} is null or empty,
   *                                  if {@code resourceList} is null,
   *                                  if {@code priority} is out of the allowed range,
   *                                  if {@code startTime} or {@code endTime} is invalid,
   *                                  or if {@code latitude} or {@code longitude} is out of bounds
   */
  public Task(String taskId, Map<ResourceType, Integer> resourceList, int priority,
        LocalDateTime startTime, LocalDateTime endTime, double latitude, double longitude) {
    if (taskId == null || taskId.trim().isEmpty()) {
      throw new IllegalArgumentException("Task ID cannot be null or empty.");
    }
    if (resourceList == null) {
      throw new IllegalArgumentException("Resource list cannot be null.");
    }
    validatePriority(priority);
    validateStartEndTimes(startTime, endTime);

    this.taskId = taskId;
    this.resourceList = resourceList;
    this.priority = priority;
    this.startTime = startTime;
    this.endTime = endTime;
    this.location = new Location(latitude, longitude);
  }

  /**
   * Validates that the priority is within the allowed range.
   *
   * @param priority the priority value to be validated
   * @throws IllegalArgumentException if {@code priority} is not between 1 and 5
   */
  private void validatePriority(int priority) {
    if (priority < 1 || priority > 5) {
      throw new IllegalArgumentException("Priority must be an integer between 1 and 5.");
    }
  }

  /**
   * Validates the start and end times of the task. The times are truncated to minutes before
   * comparison to prevent potential precision issues with nanoseconds, which is the default
   * precision for LocalDateTime.
   *
   * @param startTime the start time of the task
   * @param endTime   the end time of the task
   * @throws IllegalArgumentException if the start time or end time is invalid
   */
  private void validateStartEndTimes(LocalDateTime startTime, LocalDateTime endTime) {
    // Capture current time and truncate to minutes for comparison
    final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    if (startTime == null) {
      throw new IllegalArgumentException("Start time cannot be null.");
    }
    if (endTime == null) {
      throw new IllegalArgumentException("End time cannot be null.");
    }

    // Truncate startTime and endTime to minutes as well, for the same precision during comparison
    startTime = startTime.truncatedTo(ChronoUnit.MINUTES);
    endTime = endTime.truncatedTo(ChronoUnit.MINUTES);

    if (startTime.isBefore(now)) {
      throw new IllegalArgumentException("Start time cannot be in the past.");
    }
    if (endTime.isBefore(now)) {
      throw new IllegalArgumentException("End time cannot be in the past.");
    }
    if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
      throw new IllegalArgumentException("End time cannot be before or same as the start time.");
    }
  }

  /**
   * Updates the priority of the task.
   *
   * @param priority the new priority of the task
   * @throws IllegalArgumentException if {@code priority} is not between 1 and 5
   */
  public void updatePriority(int priority) {
    validatePriority(priority);
    this.priority = priority;
  }

  /**
   * Updates the start time and/or end time of the task.
   *
   * @param startTime the new time that task starts
   * @param endTime   the new time that task ends
   * @throws IllegalArgumentException if either {@code startTime} or {@code endTime} is invalid
   */
  public void updateStartAndEndTime(LocalDateTime startTime, LocalDateTime endTime) {
    validateStartEndTimes(startTime, endTime);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * Updates the quantity of or add a resource needed for the task.
   *
   * @param resourceType  the type of resource needed
   * @param quantity      the quantity of the resource needed
   * @throws IllegalArgumentException if {@code resourceType} is null 
   *                                  or {@code quantity} is negative
   */
  public void updateResource(ResourceType resourceType, int quantity) {
    if (resourceType == null) {
      throw new IllegalArgumentException("Resource type cannot be null.");
    }
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative.");
    }
    
    if (resourceList.containsKey(resourceType)) {
      resourceList.replace(resourceType, quantity);
    } else {
      resourceList.put(resourceType, quantity);
    }
  }

  /**
   * Returns a string representation of the task, including its id, location,
   * start and end time, priority and the resources needed.
   *
   * @return A string representing the task.
   */
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Task ID: ").append(taskId).append("; ")
            .append("Location: ").append(location.getCoordinates()).append("; ")
            .append("Start: ").append(startTime.toString()).append("; ")
            .append("End: ").append(endTime.toString()).append("; ")
            .append("Priority: ").append(String.valueOf(priority)).append("\n")
            .append("Resources Needed: \n");
    for (Map.Entry<ResourceType, Integer> entry : resourceList.entrySet()) {
      ResourceType key = entry.getKey();
      Integer value = entry.getValue();
      result.append(String.valueOf(value)).append(" of the following resource type is needed: \n")
              .append(key.toString()).append("\n");
    }
    return result.toString();
  }

  /**
   * Updates the location of the task.
   *
   * @param latitude  the new latitude of the task's location
   * @param longitude the new longitude of the task's location
   * @throws IllegalArgumentException if {@code latitude} or {@code longitude} are out of bounds
   */
  public void updateLocation(double latitude, double longitude) {
    this.location = new Location(latitude, longitude);
  }

  public Map<ResourceType, Integer> getResources() {
    return resourceList;
  }

  public String getTaskId() {
    return taskId;
  }

  public Location getLocation() {
    return location;
  }

  public int getPriority() {
    return priority;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

}
