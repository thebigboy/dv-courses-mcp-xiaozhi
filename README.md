# Spring AI MCP Server for Course Information

## Overview

This repository contains a Spring Boot application that implements the Model Control Protocol (MCP) server for providing course information. The application creates a lightweight server that can expose course data through the Spring AI MCP framework, allowing AI models to interact with your custom data services using standardized tooling.

The server exposes two main tools:
- A tool to retrieve all available courses
- A tool to search for specific courses by title

This implementation serves as an excellent starting point for creating your own Model Control Protocol servers or for integrating external data sources with AI models through Spring AI.

## Project Requirements

- Java 24
- Maven 3.8+
- Spring Boot 3.4.4
- Spring AI 1.0.0-M6

## Dependencies

The project relies on the following key dependencies:

- **Spring AI MCP Server**: Provides the foundation for creating MCP-compatible servers
  ```xml
  <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
  </dependency>
  ```

- **Spring Boot Test**: For testing the application
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
  </dependency>
  ```

## Getting Started

### Prerequisites

Before running the application, make sure you have:
- Java 24 installed on your system
- Maven installed for dependency management
- Basic understanding of Spring Boot applications

### Setting Up the Project

1. Review the project structure to understand the components:
    - `Course.java`: A simple record representing course data
    - `CourseService.java`: Service with MCP tool annotations
    - `CoursesApplication.java`: Main application class with tool registration
    - `application.properties`: Configuration for the MCP server

2. The application is configured to run as a non-web application using STDIO transport for MCP communication:
   ```properties
   spring.main.web-application-type=none
   spring.ai.mcp.server.name=dan-vega-mcp
   spring.ai.mcp.server.version=0.0.1
   
   # These settings are critical for STDIO transport
   spring.main.banner-mode=off
   logging.pattern.console=
   ```

## How to Run the Application

Running the application is straightforward with Maven:

```bash
mvn spring-boot:run
```

The application will start as a Model Control Protocol server accessible via standard input/output. It doesn't open any network ports or provide a web interface, as indicated by the `spring.main.web-application-type=none` configuration.

When running, the server registers two tools with the MCP:
- `dv_get_courses`: Returns all available courses
- `dv_get_course`: Returns a specific course by title

## Understanding the Code

### Defining Data Models

The application uses a simple record to represent course data:

```java
public record Course(String title, String url) {
}
```

This immutable data structure provides a clean way to represent course information with title and URL attributes.

### Implementing Tool Functions

The `CourseService` class demonstrates how to create MCP tools using the `@Tool` annotation:

```java
@Service
public class CourseService {
    private List<Course> courses = new ArrayList<>();

    @Tool(name = "dv_get_courses", description = "Get a list of courses from Dan Vega")
    public List<Course> getCourses() {
        return courses;
    }

    @Tool(name = "dv_get_course", description = "Get a single courses from Dan Vega by title")
    public Course getCourse(String title) {
        return courses.stream()
                .filter(course -> course.title().equals(title))
                .findFirst()
                .orElse(null);
    }

    @PostConstruct
    public void init() {
        courses.addAll(List.of(
                new Course("Building Web Applications with Spring Boot (FreeCodeCamp)", 
                          "https://youtu.be/31KTdfRH6nY"),
                new Course("Spring Boot Tutorial for Beginners - 2023 Crash Course using Spring Boot 3",
                          "https://youtu.be/UgX5lgv4uVM")
        ));
    }
}
```

The `@Tool` annotation transforms regular methods into MCP-compatible tools with:
- A unique name for identification
- A description that helps AI models understand the tool's purpose

### Registering Tools with MCP

In the main application class, tools are registered with the MCP framework:

```java
@SpringBootApplication
public class CoursesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoursesApplication.class, args);
    }

    @Bean
    public List<ToolCallback> danTools(CourseService courseService) {
        return List.of(ToolCallbacks.from(courseService));
    }
}
```

The `ToolCallbacks.from()` method scans the service class for `@Tool` annotations and registers them with the MCP framework.

## Extending the Project

You can extend this project in several ways:

1. **Add more courses**: Modify the `init()` method in `CourseService` to include additional courses.

2. **Create new tool functions**: Add more methods with the `@Tool` annotation to expose additional functionality.

3. **Implement database storage**: Replace the in-memory list with a database connection to store course information persistently.

4. **Add search capabilities**: Implement more advanced search functions beyond exact title matching.

Example of adding a search function:

```java
@Tool(name = "dv_search_courses", description = "Search courses containing a keyword")
public List<Course> searchCourses(String keyword) {
    return courses.stream()
            .filter(course -> course.title().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
}
```

## Using the MCP Server with AI Models

To utilize this MCP server with AI models:

1. Ensure your AI framework supports the Model Control Protocol
2. Connect the AI model to the MCP server using STDIO transport
3. The AI model can then invoke the exposed tools:
    - Request a list of all courses
    - Retrieve details about a specific course by title

This allows AI models to access real-time course information and provide it in responses to user queries.

### Configuration for Claude Desktop Client

To use this MCP server with the Claude Desktop client, you need to add configuration to tell Claude where to find the server. Add the following configuration to your Claude Desktop setup:

```json
{
  "dan-vega-mcp": {
    "command": "/Users/vega/.sdkman/candidates/java/current/bin/java",
    "args": [
      "-jar",
      "/Users/vega/Downloads/courses/target/courses-0.0.1-SNAPSHOT.jar"
    ]
  }
}
```

This configuration:
- Creates a tool named "dan-vega-mcp" in Claude Desktop
- Specifies the path to your Java executable
- Provides arguments to run the compiled JAR file

Make sure to adjust the paths to match your specific environment:
- Update the Java path to match your installation
- Update the JAR file path to where your compiled application is located

## Conclusion

This Spring AI MCP Server provides a clean, extensible framework for exposing course data through the Model Control Protocol. By following the Spring AI conventions and leveraging the tool annotation system, you can create powerful integrations between AI models and your data services.

The project demonstrates how to structure your code for MCP compatibility while maintaining good software design practices. With this foundation, you can build more complex data providers that enhance AI capabilities with access to custom, domain-specific information.

For more information about Spring AI and the Model Control Protocol, refer to the official documentation.