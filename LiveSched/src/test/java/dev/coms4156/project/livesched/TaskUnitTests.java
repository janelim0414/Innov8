package dev.coms4156.project.livesched;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Unit tests for the Task class.
 */
@SpringBootTest
@ContextConfiguration
public class TaskUnitTests {

  private Task testTask;
  private final String taskId = "1";
  private final String taskName = "TestTask";
  private final double latitude = 40.81;
  private final double longitude = -73.96;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private ResourceType resourceType;
  private Map<ResourceType, Integer> resourceList;

  /**
   * Set up to be run before all tests.
   */
  @BeforeEach
  void setupTaskForTesting() {
    startTime = LocalDateTime.now();
    endTime = startTime.plusHours(1);
    resourceList = new HashMap<>();
    resourceType = new ResourceType("testResourceType", 5, latitude, longitude);
    resourceList.put(resourceType, 2);
    testTask = new Task(taskId, taskName, resourceList, 3, startTime, endTime, latitude, longitude);
  }

  /**
   * Test for the constructor and parameter validation in Task class.
   */
  @Test
  void constructorTest() {
    assertDoesNotThrow(() ->
            new Task(taskId, taskName, resourceList, 3, startTime, endTime, latitude, longitude),
        "Task constructor should not throw an exception with valid parameters.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(null, taskName, resourceList, 3, startTime, endTime, latitude, longitude),
        "Task constructor should throw an exception if task ID is null.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task("  ", taskName, resourceList, 3, startTime, endTime, latitude, longitude),
        "Task constructor should throw an exception if task ID is empty.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task("", taskName, resourceList, 3, startTime, endTime, latitude, longitude),
            "Task constructor should throw an exception if task ID is empty.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, null, resourceList, 3, startTime, endTime, latitude, longitude),
            "Task constructor should throw an exception if task name is null.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, " ", resourceList, 3, startTime, endTime, latitude, longitude),
            "Task constructor should throw an exception if task name is empty.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, "", resourceList, 3, startTime, endTime, latitude, longitude),
            "Task constructor should throw an exception if task name is empty.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, null, 3, startTime, endTime, latitude, longitude),
        "Task constructor should throw an exception if resourceList is null.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, -1, startTime, endTime, latitude, longitude),
        "Task constructor should throw an exception if priority is less than 1.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 6, startTime, endTime, latitude, longitude),
        "Task constructor should throw an exception if priority is greater than 5.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, null, endTime, latitude, longitude),
        "Task constructor should throw an exception if startTime is null.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, LocalDateTime.now().minusHours(1), endTime,
                latitude, longitude),
        "Task constructor should throw an exception if startTime is in the past.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, null, latitude, longitude),
        "Task constructor should throw an exception if endTime is null.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime,
                LocalDateTime.now().minusHours(1), latitude, longitude),
        "Task constructor should throw an exception if endTime is in the past.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, startTime.minusMinutes(10),
                latitude, longitude),
        "Task constructor should throw an exception if endTime is before startTime.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, startTime, latitude, longitude),
        "Task constructor should throw an exception if endTime is the same as startTime.");

    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, startTime, -100, longitude),
            "Task constructor should throw an exception if latitude is less than -90.");
    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, startTime, 100, longitude),
            "Task constructor should throw an exception if latitude is greater than 90.");
    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, startTime, latitude, -200),
            "Task constructor should throw an exception if latitude is less than -180.");
    assertThrows(IllegalArgumentException.class, () ->
            new Task(taskId, taskName, resourceList, 3, startTime, startTime, latitude, 200),
            "Task constructor should throw an exception if latitude is greater than 180.");
  }

  /**
   * Test for getTaskId method in Task class.
   */
  @Test
  void getTaskIdTest() {
    assertEquals(taskId, testTask.getTaskId(), "Task ID should match.");
  }

  /**
   * Test for getResources method in Task class.
   */
  @Test
  void getResourcesTest() {
    assertEquals(resourceList, testTask.getResources(), "Resource list should match.");
  }

  /**
   * Test for getLocation method in Task class.
   */
  @Test
  void getLocationTest() {
    Location location = new Location(latitude, longitude);
    assertEquals(location.getCoordinates(), testTask.getLocation().getCoordinates(),
            "Location should match.");
  }

  /**
   * Test for getPriority method in Task class.
   */
  @Test
  void getPriorityTest() {
    assertEquals(3, testTask.getPriority(), "Priority should match.");
  }

  /**
   * Test for getStartTime method in Task class.
   */
  @Test
  void getStartTimeTest() {
    assertEquals(startTime, testTask.getStartTime(), "Start time should match.");
  }

  /**
   * Test for getEndTime method in Task class.
   */
  @Test
  void getEndTimeTest() {
    assertEquals(endTime, testTask.getEndTime(), "End time should match.");
  }

  /**
   * Test for updatePriority method in Task class.
   */
  @Test
  void updatePriorityTest() {
    testTask.updatePriority(1);
    assertEquals(1, testTask.getPriority(), "Priority should be updated to 1.");

    assertThrows(IllegalArgumentException.class, () -> testTask.updatePriority(0),
        "Priority should not be updated to a value less than 1.");

    assertThrows(IllegalArgumentException.class, () -> testTask.updatePriority(7),
        "Priority should not be updated to a value greater than 5.");
  }

  /**
   * Test for updateStartAndEndTime method in Task class.
   */
  @Test
  void updateStartAndEndTimeTest() {
    LocalDateTime newStartTime = startTime.plusDays(1);
    LocalDateTime newEndTime = endTime.plusDays(1);

    testTask.updateStartAndEndTime(newStartTime, newEndTime);
    assertEquals(newStartTime, testTask.getStartTime(), "Start time should be updated.");
    assertEquals(newEndTime, testTask.getEndTime(), "End time should be updated.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateStartAndEndTime(null, newEndTime),
        "Start time should not be updated when null is given.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateStartAndEndTime(LocalDateTime.now().minusDays(1), newEndTime),
        "Start time should not be updated to a time in the past.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateStartAndEndTime(newStartTime, null),
        "End time should not be updated when null is given.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateStartAndEndTime(newStartTime, LocalDateTime.now().minusDays(2)),
        "End time should not be updated to a time in the past.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateStartAndEndTime(newStartTime, endTime),
        "End time should not be updated to a time before the start time.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateStartAndEndTime(newStartTime, newStartTime),
        "End time should not be updated to a time equal to the start time.");
  }

  /**
   * Test for updateResource method in Task class.
   */
  @Test
  void updateResourceTest() {
    testTask.updateResource(resourceType, 5);

    assertEquals(5, testTask.getResources().get(resourceType).intValue(),
        "Quantity for the existing resource type should be updated to 5.");

    ResourceType newResourceType = new ResourceType("NewType", 10, latitude, longitude);
    testTask.updateResource(newResourceType, 3);

    assertTrue(testTask.getResources().containsKey(newResourceType),
        "New resource type should be added to the resource list.");
    assertEquals(3, testTask.getResources().get(newResourceType).intValue(),
        "Quantity for the new resource type should be 3.");

    assertThrows(IllegalArgumentException.class, () ->
        testTask.updateResource(null, 3),
        "Resource type to add should not be null.");

    assertThrows(IllegalArgumentException.class, () ->
        testTask.updateResource(newResourceType, -1),
        "Quantity for new resource type should not be negative.");

    testTask.updateResource(newResourceType, 0);

    assertFalse(testTask.getResources().containsKey(newResourceType),
        "New resource type should be removed from the resource list.");
  }

  /**
   * Test for updateLocation method in Task class.
   */
  @Test
  void updateLocationTest() {
    double newLatitude = 37.77;
    double newLongitude = -122.41;
    Location newLocation = new Location(newLatitude, newLongitude);

    testTask.updateLocation(newLatitude, newLongitude);
    assertEquals(newLocation.getCoordinates(), testTask.getLocation().getCoordinates(),
        "Location of task should be updated to the new location.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateLocation(91.0, longitude),
        "Latitude out of bounds should throw an exception.");

    assertThrows(IllegalArgumentException.class, () ->
            testTask.updateLocation(latitude, -200.0),
        "Longitude out of bounds should throw an exception.");
  }

  /**
   * Test for equals method in Task class.
   */
  @Test
  void equalsTest() {
    assertTrue(testTask.equals(testTask),
        "A task must be equal to itself.");

    assertFalse(testTask.equals(null),
        "A task must not be equal to null.");

    assertFalse(testTask.equals("Some String"),
        "A task must not be equal to an object of a different class.");

    Task sameTaskId = new Task(taskId, "DifferentName", resourceList, 3,
        startTime, endTime, latitude, longitude);
    assertTrue(testTask.equals(sameTaskId),
        "Two tasks with the same taskId should be equal regardless of other attributes.");

    Task differentTaskId = new Task("2", taskName, resourceList, 3,
        startTime, endTime, latitude, longitude);
    assertFalse(testTask.equals(differentTaskId),
        "Two tasks with different taskIds must not be equal.");
  }

  @Test
  void hashCodeTest() {
    int expectedResult = Objects.hash(taskId);
    assertEquals(expectedResult, testTask.hashCode(), "Hashcode should be " + expectedResult);
  }

  @Test
  void getTaskNameTest() {
    String expectedResult = taskName;
    assertEquals(expectedResult, testTask.getTaskName(), "Task name should be " + taskName);
  }
}
