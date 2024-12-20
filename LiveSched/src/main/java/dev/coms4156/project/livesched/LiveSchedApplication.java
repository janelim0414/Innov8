package dev.coms4156.project.livesched;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Class contains all the startup logic for the application.
 */
@SpringBootApplication
public class LiveSchedApplication implements CommandLineRunner {

  /**
   * The main launcher for the services all it does
   * is make a call to the overridden run method.
   *
   * @param args A {@code String[]} of any potential
   *             runtime arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(LiveSchedApplication.class, args);
  }

  /**
   * This contains all the setup logic, it will mainly be focused
   * on loading up and creating an instance of the database based
   * off a saved file or will create a fresh database if the file
   * is not present.
   *
   * @param args A {@code String[]} of any potential runtime args
   */
  @Override
  public void run(String[] args) {
    boolean isSetupMode = false;
    clientDatabases = new HashMap<>();

    for (String arg : args) {
      switch (arg.split("=")[0]) {
        case "setup":
          isSetupMode = true;
          break;
        case "--useGCS":
          useGCS = true;
          System.out.println("GCS operations enabled.");
          break;
        default:
          System.out.println("Unknown argument: " + arg);
          break;
      }
    }

    // Reload existing client databases
    if (useGCS) {
      reloadClientDatabasesCloud();
    } else {
      reloadClientDatabasesLocal();
    }

    if (isSetupMode) {
      setupExampleClientDatabase("demoClientId");
      System.out.println("Example data setup completed for client ID: demoClientId");
    }

    System.out.println("LiveSched service started");
  }

  /**
   * Overrides the client databases, used when testing.
   *
   * @param testData  A {@code MyFileDatabase} object referencing test data.
   * @param clientId  A {@code String} object referencing the client ID.
   */
  public static void overrideDatabase(MyFileDatabase testData, String clientId) {
    clientDatabases.put(clientId, testData);
    saveData = false;
  }

  /**
   * Restores the client databases, used when testing.
   *
   * @param clientId  A {@code String} object referencing the client ID.
   */
  public static void restoreDatabase(String clientId) {
    clientDatabases.remove(clientId);
    saveData = true;
  }

  /**
   * Reloads client databases from Google Cloud Storage (GCS).
   */
  public void reloadClientDatabasesCloud() {
    try {
      // List objects in the bucket
      Page<Blob> blobs = storage.list("innov8-livesched-bucket",
          Storage.BlobListOption.prefix("gcs_"),
          Storage.BlobListOption.currentDirectory());

      // Loop through all objects to find matching files
      Map<String, String> clientFiles = new HashMap<>();
      for (Blob blob : blobs.iterateAll()) {
        String name = blob.getName();
        if (name.contains("_tasks.txt")) {
          String clientId = name.substring(4, name.indexOf("_tasks.txt"));
          clientFiles.put(clientId, "tasks");
        } else if (name.contains("_resourceTypes.txt")) {
          String clientId = name.substring(4, name.indexOf("_resourceTypes.txt"));
          clientFiles.put(clientId, "resourceTypes");
        } else if (name.contains("_schedules.txt")) {
          String clientId = name.substring(4, name.indexOf("_schedules.txt"));
          clientFiles.put(clientId, "schedules");
        }
      }

      // Load data for all existing clients
      for (String clientId : clientFiles.keySet()) {
        String taskFilePath = generateClientFilePath(clientId, TASK_FILE_PATH);
        String resourceTypeFilePath = generateClientFilePath(clientId, RESOURCE_TYPE_FILE_PATH);
        String scheduleFilePath = generateClientFilePath(clientId, SCHEDULE_FILE_PATH);

        String taskObjectName = generateClientObjectName(clientId, TASK_FILE_PATH);
        String resourceObjectName = generateClientObjectName(clientId, RESOURCE_TYPE_FILE_PATH);
        String scheduleObjectName = generateClientObjectName(clientId, SCHEDULE_FILE_PATH);

        MyFileDatabase database = new MyFileDatabase(0, taskFilePath, resourceTypeFilePath,
            scheduleFilePath, taskObjectName, resourceObjectName, scheduleObjectName);

        clientDatabases.put(clientId, database);
        System.out.println("Loaded database for client ID (GCS): " + clientId);
      }
    } catch (Exception e) {
      System.out.println("Error accessing GCS bucket: " + e.getMessage());
    }
  }

  /**
   * Reloads client databases from local file system.
   */
  public void reloadClientDatabasesLocal() {
    File tmpDir = new File("/tmp");
    if (!tmpDir.exists() || !tmpDir.isDirectory()) {
      System.out.println("No existing databases found.");
      return;
    }

    // Scan tmp directory for files matching the pattern clientId_tasks.txt
    File[] taskFiles = tmpDir.listFiles((dir, name) -> name.endsWith(TASK_FILE_PATH));
    if (taskFiles == null || taskFiles.length == 0) {
      System.out.println("No existing task files found.");
      return;
    }

    // Load data for all existing clients
    for (File taskFile : taskFiles) {
      String fileName = taskFile.getName();
      String clientId = fileName.substring(0, fileName.indexOf("_tasks.txt")); // Get clientId

      String taskFilePath = generateClientFilePath(clientId, TASK_FILE_PATH);
      String resourceTypeFilePath = generateClientFilePath(clientId, RESOURCE_TYPE_FILE_PATH);
      String scheduleFilePath = generateClientFilePath(clientId, SCHEDULE_FILE_PATH);

      String taskObjectName = generateClientObjectName(clientId, TASK_FILE_PATH);
      String resourceObjectName = generateClientObjectName(clientId, RESOURCE_TYPE_FILE_PATH);
      String scheduleObjectName = generateClientObjectName(clientId, SCHEDULE_FILE_PATH);

      MyFileDatabase database = new MyFileDatabase(0, taskFilePath, resourceTypeFilePath,
          scheduleFilePath, taskObjectName, resourceObjectName, scheduleObjectName);

      clientDatabases.put(clientId, database);
      System.out.println("Loaded database for client ID (Local): " + clientId);
    }
  }

  /**
   * Retrieves the database instance associated with the specified client ID.
   *
   * @param clientId  A {@code String} the identifier for the client whose database is retrieved
   *
   * @return the {@code MyFileDatabase} instance associated with the specified client ID
   */
  public static synchronized MyFileDatabase getClientFileDatabase(String clientId) {
    if (!clientDatabases.containsKey(clientId)) {
      System.out.println("Initializing a new database for client ID: " + clientId);

      // Generate file paths and object names for new client
      String taskFilePath = generateClientFilePath(clientId, TASK_FILE_PATH);
      String resourceTypeFilePath = generateClientFilePath(clientId, RESOURCE_TYPE_FILE_PATH);
      String scheduleFilePath = generateClientFilePath(clientId, SCHEDULE_FILE_PATH);

      String taskObjectName = generateClientObjectName(clientId, TASK_FILE_PATH);
      String resourceObjectName = generateClientObjectName(clientId, RESOURCE_TYPE_FILE_PATH);
      String scheduleObjectName = generateClientObjectName(clientId, SCHEDULE_FILE_PATH);

      MyFileDatabase myFileDatabase = new MyFileDatabase(1, taskFilePath, resourceTypeFilePath,
          scheduleFilePath, taskObjectName, resourceObjectName, scheduleObjectName);

      clientDatabases.put(clientId, myFileDatabase);
    }

    return clientDatabases.get(clientId);
  }

  /**
   * Creates an example database for demo purposes.
   *
   * @param clientId  The client ID to create an example database for
   */
  public void setupExampleClientDatabase(String clientId) {
    if (clientId == null) {
      throw new IllegalArgumentException("ClientId is null");
    }
    if (clientId.trim().isEmpty()) {
      throw new IllegalArgumentException("ClientId is empty");
    }
    // Generate file paths and object names for demo
    String taskFilePath = generateClientFilePath(clientId, TASK_FILE_PATH);
    String resourceTypeFilePath = generateClientFilePath(clientId, RESOURCE_TYPE_FILE_PATH);
    String scheduleFilePath = generateClientFilePath(clientId, SCHEDULE_FILE_PATH);

    String taskObjectName = generateClientObjectName(clientId, TASK_FILE_PATH);
    String resourceObjectName = generateClientObjectName(clientId, RESOURCE_TYPE_FILE_PATH);
    String scheduleObjectName = generateClientObjectName(clientId, SCHEDULE_FILE_PATH);

    MyFileDatabase demoDatabase = new MyFileDatabase(1, taskFilePath, resourceTypeFilePath,
        scheduleFilePath, taskObjectName, resourceObjectName, scheduleObjectName);

    setupExampleData(demoDatabase); // Load database with example resources and tasks
    clientDatabases.put(clientId, demoDatabase);
  }

  /**
   * Populates the database with some example resources and tasks.
   *
   * @param myFileDatabase The database to generate example data for
   */
  public void setupExampleData(MyFileDatabase myFileDatabase) {
    if (myFileDatabase == null) {
      throw new IllegalArgumentException("MyFileDatabase is null");
    }
    ResourceType bed = new ResourceType("Bed", 20, 40.84, -73.94);
    ResourceType nurse = new ResourceType("Nurse", 15, 40.84, -73.94);
    ResourceType doctor = new ResourceType("Doctor", 10, 40.84, -73.94);
    ResourceType ambulance = new ResourceType("Ambulance", 5, 40.84, -73.94);

    List<ResourceType> allResourceTypes = new ArrayList<>();
    allResourceTypes.add(bed);
    allResourceTypes.add(nurse);
    allResourceTypes.add(doctor);
    allResourceTypes.add(ambulance);

    Map<ResourceType, Integer> emergencyResources = new HashMap<>();
    emergencyResources.put(bed, 2); // for two people at the same time
    emergencyResources.put(doctor, 2);
    emergencyResources.put(nurse, 2);

    Task emergency = new Task(
        "1", "ER", emergencyResources, 1,
        LocalDateTime.of(2024, 12, 17, 10, 00), LocalDateTime.of(2024, 12, 17, 13, 00),
        40.81, -73.96);

    List<Task> allTasks = new ArrayList<>();
    allTasks.add(emergency);

    Map<ResourceType, Integer> checkupResources = new HashMap<>();
    checkupResources.put(nurse, 1);
    checkupResources.put(doctor, 1);

    Task checkup = new Task(
        "2", "checkup", checkupResources, 3,
        LocalDateTime.of(2024, 12, 19, 10, 00), LocalDateTime.of(2024, 12, 19, 10, 30),
        40.81, -73.96);

    allTasks.add(checkup);

    Map<ResourceType, Integer> transportResources = new HashMap<>();
    transportResources.put(ambulance, 1);
    transportResources.put(nurse, 1);

    Task patientTransport = new Task(
        "3", "transport", transportResources, 2,
        LocalDateTime.of(2024, 12, 17, 10, 15), LocalDateTime.of(2024, 12, 17, 10, 45),
        40.83, -73.91);

    allTasks.add(patientTransport);

    myFileDatabase.setAllResourceTypes(allResourceTypes);
    myFileDatabase.setAllTasks(allTasks);
  }

  /**
   * Generates file paths based on the client ID.
   *
   * @param clientId The unique identifier for the client
   * @param fileName The name of the file
   * @return The full path for the file
   */
  public static String generateClientFilePath(String clientId, String fileName) {
    if (clientId == null || fileName == null) {
      throw new IllegalArgumentException("clientId and fileName cannot be null");
    }
    if (clientId.trim().isEmpty() || fileName.trim().isEmpty()) {
      throw new IllegalArgumentException("clientId and fileName cannot be empty");
    }
    return "/tmp/" + clientId + "_" + fileName;
  }

  /**
   * Generates gcs object name based on the client ID.
   *
   * @param clientId The unique identifier for the client
   * @param fileName The name of the file
   * @return The full name for the object
   */
  private static String generateClientObjectName(String clientId, String fileName) {
    return "gcs_" + clientId + "_" + fileName;
  }

  /**
   * This contains all the overheading teardown logic, it will
   * mainly be focused on saving all the created user data to a
   * file, so it will be ready for the next setup.
   */
  @PreDestroy
  public void onTermination() {
    System.out.println("Termination");
    if (saveData) {
      for (Map.Entry<String, MyFileDatabase> entry : clientDatabases.entrySet()) {
        String clientId = entry.getKey();
        System.out.println("Saving data for client ID: " + clientId);

        MyFileDatabase database = entry.getValue();
        database.saveContentsToFile(1); // Save tasks
        database.saveContentsToFile(2); // Save resourceTypes
        database.saveContentsToFile(3); // Save schedule
      }
    }
  }

  public static Map<String, MyFileDatabase> clientDatabases;
  public static boolean useGCS = false; // Default is local mode (Not use Google Cloud Storage)

  private final Storage storage = StorageOptions.getDefaultInstance().getService();
  private static final String TASK_FILE_PATH = "tasks.txt";
  private static final String RESOURCE_TYPE_FILE_PATH = "resourceTypes.txt";
  private static final String SCHEDULE_FILE_PATH = "schedules.txt";
  private static final String APP_ENGINE_ENV = "standard"; // Constant for environment check
  private static boolean saveData = true;

  // Detect App Engine environment and enable GCS if running in App Engine
  static {
    String env = System.getenv("GAE_ENV");
    if (APP_ENGINE_ENV.equals(env)) {
      useGCS = true;
      System.out.println("Running in App Engine: GCS enabled.");
    }
  }
}
