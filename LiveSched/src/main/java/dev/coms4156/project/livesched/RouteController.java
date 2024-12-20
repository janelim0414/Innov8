package dev.coms4156.project.livesched;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class contains all the API routes for the system.
 */
@RestController
public class RouteController {

  private static final String TASK_ID = "taskId";
  private static final String CLIENT_ID = "clientId";
  private static final String TASK_NOT_FOUND = "Task Not Found";

  /**
   * Redirects to the homepage.
   *
   * @return A String containing the name of the html file to be loaded.
   */
  @GetMapping({"/", "/index", "/home"})
  public String index() {
    return "Welcome, in order to make an API call direct your browser or Postman to an endpoint "
            + "\n\n This can be done using the following format: \n\n http:127.0.0"
            + ".1:8080/endpoint?arg=value";
  }

  /**
   * Returns the details of all tasks in the database.
   *
   * @param clientId A {@code String} representing the client for whom to retreive tasks
   *
   * @return A {@code ResponseEntity} object containing either a list of all Tasks and
   *         an HTTP 200 response, or an appropriate message indicating the proper response.
   */
  @GetMapping(value = "/retrieveTasks", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> retrieveTasks(@RequestParam(value = CLIENT_ID) String clientId) {
    try {
      List<Task> taskList = LiveSchedApplication.getClientFileDatabase(clientId).getAllTasks();

      if (taskList == null || taskList.isEmpty()) {
        return new ResponseEntity<>("Tasks Not Found", HttpStatus.NOT_FOUND);
      } else {
        return new ResponseEntity<>(taskList, HttpStatus.OK);
      }

    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Returns the details of a specified task in the database.
   *
   * @param taskId     A {@code String} representing the task the user wishes
   *                   to retrieve.
   * @param clientId   A {@code String} representing the client for whom to retreive the task
   *
   * @return A {@code ResponseEntity} object containing either the details of the Task and
   *         an HTTP 200 response or, an appropriate message indicating the proper response.
   */
  @GetMapping(value = "/retrieveTask", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> retrieveTask(@RequestParam(value = TASK_ID) String taskId,
                                        @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      Task task = LiveSchedApplication.getClientFileDatabase(clientId).getTaskById(taskId);

      if (task == null) {
        return new ResponseEntity<>(TASK_NOT_FOUND, HttpStatus.NOT_FOUND);
      } else {
        return new ResponseEntity<>(task, HttpStatus.OK);
      }

    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Returns the details of all resource types in the database.
   *
   * @param clientId A {@code String} representing the client that owns the schedules
   *
   * @return A {@code ResponseEntity} object containing either the details of the ResourceTypes and
   *         an HTTP 200 response or, an appropriate message indicating the proper response.
   */
  @GetMapping(value = "/retrieveResourceTypes", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> retrieveResourceTypes(@RequestParam(value = CLIENT_ID) String clientId) {
    try {
      List<ResourceType> resourceTypeList;
      resourceTypeList = LiveSchedApplication.getClientFileDatabase(clientId).getAllResourceTypes();

      if (resourceTypeList.isEmpty()) {
        return new ResponseEntity<>("ResourceTypes Not Found", HttpStatus.NOT_FOUND);
      } else {
        return new ResponseEntity<>(resourceTypeList, HttpStatus.OK);
      }

    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Returns the details of the master schedule in the database.
   *
   * @param clientId       A {@code String} representing the client that owns the schedule
   *
   * @return A {@code ResponseEntity} object containing the master schedule and
   *         an HTTP 200 response or, an appropriate message indicating the proper response.
   */
  @GetMapping(value = "/retrieveSchedule", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> retrieveSchedule(@RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      Schedule masterSchedule = myFileDatabase.getMasterSchedule();

      if (masterSchedule == null || masterSchedule.getTaskSchedule().isEmpty()) {
        return new ResponseEntity<>("Schedules Not Found", HttpStatus.NOT_FOUND);
      }

      // Convert schedule to have a cleaner JSON structure
      List<Map<String, Object>> scheduleList = new ArrayList<>();
      masterSchedule.getTaskSchedule().forEach((task, resources) -> {
        Map<String, Object> scheduleEntry = new HashMap<>();
        scheduleEntry.put("task", task);
        scheduleEntry.put("assignedResources", resources);
        scheduleList.add(scheduleEntry);
      });

      return new ResponseEntity<>(scheduleList, HttpStatus.OK);

    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Update and returns the schedule for current tasks and resources.
   *
   * @param maxDistance    A {@code double} representing the max distance
   *                       the user wishes between schedule tasks and resources.
   * @param clientId      A {@code String} representing the client for whom the schedule
   *                      will be created.
   *
   * @return A {@code ResponseEntity} object containing either the details of the Schedule and
   *         an HTTP 200 response or, an appropriate message indicating the proper response.
   */
  @PatchMapping(value = "/updateSchedule", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateSchedule(@RequestParam(value = "maxDistance") double maxDistance,
                                          @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      List<Task> taskList = myFileDatabase.getAllTasks();

      if (taskList == null || taskList.isEmpty()) {
        return new ResponseEntity<>("Tasks Not Found", HttpStatus.NOT_FOUND);
      }

      Schedule masterSchedule = myFileDatabase.getMasterSchedule();

      if (masterSchedule == null) {
        return new ResponseEntity<>("Master Schedule Not Found", HttpStatus.NOT_FOUND);
      }

      Map<Task, List<Resource>> updatedSchedule =
          masterSchedule.updateSchedule(taskList, maxDistance);

      // Convert schedule to have a cleaner JSON structure
      List<Map<String, Object>> response = new ArrayList<>();
      for (Map.Entry<Task, List<Resource>> entry : updatedSchedule.entrySet()) {
        Map<String, Object> scheduleEntry = new LinkedHashMap<>();
        scheduleEntry.put("task", Map.of(
            "taskId", entry.getKey().getTaskId(),
            "taskName", entry.getKey().getTaskName(),
            "priority", entry.getKey().getPriority(),
            "startTime", entry.getKey().getStartTime(),
            "endTime", entry.getKey().getEndTime(),
            "location", entry.getKey().getLocation(),
            "resources", entry.getKey().getResources()
        ));
        scheduleEntry.put("assignedResources", entry.getValue().stream().map(resource -> Map.of(
            "resourceId", resource.getResourceId(),
            "availableFrom", resource.getAvailableFrom()
        )).toList());

        response.add(scheduleEntry);
      }

      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Attempts to add a task to the database.
   *
   * @param taskName       A {@code String} representing the name of the new task.
   * @param priority       A {@code int} representing the priority of the new task.
   * @param startTime      A {@code String} representing the start time of the new task.
   * @param endTime        A {@code String} representing the end time of the new task.
   * @param latitude       A {@code double} representing the latitude of the new task.
   * @param longitude      A {@code double} representing the longitude of the new task.
   * @param clientId       A {@code String} representing the client for whom the new task
   *
   * @return A {@code ResponseEntity} object containing the created Task object and an HTTP 200
   *          status code or the proper status code in tune with what has happened.
   */
  @PatchMapping(value = "/addTask", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> addTask(@RequestParam(value = "taskName") String taskName,
                                   @RequestParam(value = "priority") int priority,
                                   @RequestParam(value = "startTime") String startTime,
                                   @RequestParam(value = "endTime") String endTime,
                                   @RequestParam(value = "latitude") double latitude,
                                   @RequestParam(value = "longitude") double longitude,
                                   @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      String taskId = String.valueOf(myFileDatabase.getAllTasks().size() + 1);
      Map<ResourceType, Integer> resourceTypeList = new HashMap<>();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      LocalDateTime startTimeFormatted = LocalDateTime.parse(startTime, formatter);
      LocalDateTime endTimeFormatted = LocalDateTime.parse(endTime, formatter);
      Task newTask = new Task(taskId, taskName, resourceTypeList, priority,
              startTimeFormatted, endTimeFormatted, latitude, longitude);
      myFileDatabase.addTask(newTask);
      return new ResponseEntity<>(newTask, HttpStatus.OK);
    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Attempts to unschedule a task from the master schedule in database.
   *
   * @param taskId        A {@code String} representing the id of the task
   *                      to be removed from schedule.
   * @param clientId      A {@code String} representing the client owner of the schedule/task.
   *
   * @return A {@code ResponseEntity} object containing the modified Schedule and an HTTP 200
   *          status code or the proper status code in tune with what has happened.
   */
  @PatchMapping(value = "/unscheduleTask", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> unscheduleTask(@RequestParam(value = TASK_ID) String taskId,
                                          @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      Schedule masterSchedule = myFileDatabase.getMasterSchedule();
      Task task = myFileDatabase.getTaskById(taskId);

      if (task == null) {
        return new ResponseEntity<>(TASK_NOT_FOUND, HttpStatus.NOT_FOUND);
      }
      if (masterSchedule == null) {
        return new ResponseEntity<>("Master Schedule Not Found", HttpStatus.NOT_FOUND);
      }
      if (!masterSchedule.getTaskSchedule().containsKey(task)) {
        return new ResponseEntity<>("Task Not Scheduled Yet", HttpStatus.BAD_REQUEST);
      }
      masterSchedule.unscheduleTask(task);
      return new ResponseEntity<>("Task unscheduled succesfully", HttpStatus.OK);
    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Attempts to delete a task from the database.
   *
   * @param taskId           A {@code String} representing the taskId of task to be deleted.
   * @param clientId         A {@code String} representing the client that owns the task.
   *
   * @return               A {@code ResponseEntity} object containing an HTTP 200
   *                       response with an appropriate message or the proper status
   *                       code in tune with what has happened.
   */
  @DeleteMapping(value = "/deleteTask", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> deleteTask(@RequestParam(value = TASK_ID) String taskId,
                                      @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      Task task = myFileDatabase.getTaskById(taskId);
      if (task == null) {
        return new ResponseEntity<>(TASK_NOT_FOUND, HttpStatus.NOT_FOUND);
      } else {
        myFileDatabase.deleteTask(task);
        return new ResponseEntity<>(taskId + " successfully deleted", HttpStatus.OK);
      }
    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Attempts to add a resource type to the database.
   *
   * @param typeName        A {@code String} representing the name of the new resource type.
   * @param totalUnits      A {@code int} representing the number of units of its resources.
   * @param latitude        A {@code double} representing the latitude of the new resource type.
   * @param longitude       A {@code double} representing the longitude of the new resource type.
   * @param clientId        A {@code String} representing the client that owns the resource type.
   *
   * @return               A {@code ResponseEntity} object containing an HTTP 200
   *                       response with an appropriate message or the proper status
   *                       code in tune with what has happened.
   */
  @PatchMapping(value = "/addResourceType", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> addResourceType(@RequestParam(value = "typeName") String typeName,
                                           @RequestParam(value = "totalUnits") int totalUnits,
                                           @RequestParam(value = "latitude") double latitude,
                                           @RequestParam(value = "longitude") double longitude,
                                           @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      ResourceType newResourceType = new ResourceType(typeName, totalUnits, latitude, longitude);
      LiveSchedApplication.getClientFileDatabase(clientId).addResourceType(newResourceType);
      return new ResponseEntity<>("Attribute was updated successfully.", HttpStatus.OK);
    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Attempts to modify resource type for a specified task to the database.
   *
   * @param taskId          A {@code String} representing the task
   *                        the client wants to modify the resource type for.
   * @param typeName        A {@code String} representing the resource type to modify.
   * @param quantity        A {@code int} representing quantity of resource types to set.
   * @param clientId        A {@code String} representing the client that owns the resource type.
   *
   * @return               A {@code ResponseEntity} object containing an HTTP 200
   *                       response with an appropriate message or the proper status
   *                       code in tune with what has happened.
   */
  @PatchMapping(value = "/modifyResourceType", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> modifyResourceType(@RequestParam(value = TASK_ID) String taskId,
                                              @RequestParam(value = "typeName") String typeName,
                                              @RequestParam(value = "quantity") int quantity,
                                              @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      boolean doesTaskExist = retrieveTask(taskId, clientId).getStatusCode() == HttpStatus.OK;
      if (doesTaskExist) {
        List<ResourceType> resourceTypeList;
        resourceTypeList = myFileDatabase.getAllResourceTypes();
        Task task = myFileDatabase.getTaskById(taskId);
        for (ResourceType resourceType : resourceTypeList) {
          if (resourceType.getTypeName().equals(typeName)) {
            task.updateResource(resourceType, quantity);
            return new ResponseEntity<>("Attribute was updated successfully.", HttpStatus.OK);
          }
        }
        return new ResponseEntity<>("ResourceType Not Found", HttpStatus.NOT_FOUND);
      }
      return new ResponseEntity<>(TASK_NOT_FOUND, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return handleException(e);
    }
  }

  /**
   * Attempts to delete a resourceType from the database.
   *
   * @param typeName        A {@code String} representing the resource type to delete.
   * @param clientId        A {@code String} representing the client that owns the task.
   *
   * @return               A {@code ResponseEntity} object containing an HTTP 200
   *                       response with an appropriate message or the proper status
   *                       code in tune with what has happened.
   */
  @DeleteMapping(value = "/deleteResourceType", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> deleteResourceType(@RequestParam(value = "typeName") String typeName,
                                              @RequestParam(value = CLIENT_ID) String clientId) {
    try {
      MyFileDatabase myFileDatabase = LiveSchedApplication.getClientFileDatabase(clientId);
      List<Task> tasks = myFileDatabase.getAllTasks();
      List<ResourceType> resourceTypeList =
          myFileDatabase.getAllResourceTypes();
      for (ResourceType resourceType : resourceTypeList) {
        if (resourceType.getTypeName().equals(typeName)) {
          for (Task task : tasks) {
            if (task.getResources().containsKey(resourceType)) {
              return new ResponseEntity<>("Cannot delete a resourceType currently in need",
                  HttpStatus.BAD_REQUEST);
            }
          }
          myFileDatabase.deleteResourceType(resourceType);
          return new ResponseEntity<>(typeName + " successfully deleted", HttpStatus.OK);
        }
      }
      return new ResponseEntity<>("ResourceType Not Found", HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return handleException(e);
    }
  }

  private ResponseEntity<?> handleException(Exception e) {
    System.out.println(e.toString());
    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
