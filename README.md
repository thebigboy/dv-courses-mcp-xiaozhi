# Spring AI MCP Server - 多功能服务集成

基于 Spring Boot 实现的 MCP (Model Control Protocol) 服务器，集成了课程信息查询、百度地图和日历管理等多项功能，可与小智 AI 进行交互。

## 项目背景

本项目基于 [dv-courses-mcp](https://github.com/danvega/dv-courses-mcp) 改造而来。由于对 Python 不够熟悉，在尝试将多个 MCP 服务组合给小智进行交互时遇到困难，因此选择使用 Spring Boot 框架进行开发。借助 Cursor 的 AI 辅助，快速完成了开发工作，特别是在 Mac 下通过脚本创建日历功能。

## 功能演示

### MCP 连接启动效果
![image](https://github.com/user-attachments/assets/f2d1a0e0-e335-4163-898e-898e9d5f41d4)

### 对话交互效果
![image](https://github.com/user-attachments/assets/633b9cc8-d544-4924-8444-323cadc77f59)

![image](https://github.com/user-attachments/assets/078e467c-575f-4a10-956f-2f0956ef59c3)

![image](https://github.com/user-attachments/assets/967937b1-c5c8-426d-8de4-ac335d225ffb)


## 项目简介

本项目是一个基于 Spring Boot 的 MCP (Model Control Protocol) 服务器实现，提供以下核心功能：

- **课程信息查询**：获取课程列表和详细信息
- **百度地图服务**：地理位置查询和路径规划
- **日历管理**：创建和管理日历事件

通过 Spring AI MCP 框架，将这些服务统一封装为标准化的工具，供 AI 模型（如小智 AI）调用，实现智能化的数据服务交互。

## 技术栈

- Java 24
- Maven 3.8+
- Spring Boot 3.4.4
- Spring AI 1.0.0-M6
- 百度地图 API

## 核心依赖

### Spring AI MCP Server
提供 MCP 协议服务器的基础实现：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
</dependency>
```

### Spring Boot Test
用于应用测试：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 快速开始

### 环境要求

- Java 24
- Maven 3.8+
- 百度地图 API Key（[申请地址](https://lbsyun.baidu.com/apiconsole/key)）
- 小智 AI WebSocket 地址（[获取地址](https://xiaozhi.me/console)）

### 配置说明

在运行应用之前，需要在 `application.properties` 中配置以下参数：

```properties
# 小智 AI WebSocket 端点
# 从小智 AI 控制台获取: https://xiaozhi.me/console/agents/{your-agent-id}/config
endpoint=wss://your-xiaozhi-endpoint

# 百度地图 API Key
# 从百度地图开放平台获取: https://lbsyun.baidu.com/apiconsole/key
baidu.map.api.key=your-baidu-map-api-key

# MCP 服务器基础配置
spring.main.web-application-type=none
spring.ai.mcp.server.name=dan-vega-mcp
spring.ai.mcp.server.version=0.0.1

# STDIO 传输配置（关键设置）
spring.main.banner-mode=off
logging.pattern.console=
```

### 项目结构

主要组件说明：
- `Course.java`: 课程数据模型
- `CourseService.java`: 课程服务，包含 MCP 工具注解
- `CoursesApplication.java`: 主应用类，负责工具注册
- `application.properties`: MCP 服务器配置文件

### 运行应用

使用 Maven 启动应用：

```bash
mvn spring-boot:run
```

或者直接运行主类 `CoursesApplication`。

应用启动后将作为 MCP 服务器运行，通过标准输入/输出与 AI 模型通信。由于配置了 `spring.main.web-application-type=none`，应用不会开启 Web 端口或提供 HTTP 接口。

### 运行效果

<img width="1704" alt="运行效果展示" src="https://github.com/user-attachments/assets/63eb34db-ef2d-4e9d-9ba7-a4e267db0092" />

## 技术实现

### 数据模型定义

使用 Java Record 定义课程数据结构：

```java
public record Course(String title, String url) {
}
```

这个不可变的数据结构简洁地表示了课程信息，包含标题和 URL 属性。

### 实现 MCP 工具

`CourseService` 类展示了如何使用 `@Tool` 注解创建 MCP 工具：

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

`@Tool` 注解将普通方法转换为 MCP 兼容的工具，需要指定：
- **name**: 工具的唯一标识符
- **description**: 帮助 AI 模型理解工具用途的描述信息

### 注册工具到 MCP

在主应用类中，将工具注册到 MCP 框架：

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

`ToolCallbacks.from()` 方法会扫描服务类中的 `@Tool` 注解，并将它们注册到 MCP 框架中。

## 功能扩展

### 可扩展方向

1. **添加更多课程**：修改 `CourseService` 中的 `init()` 方法
2. **创建新工具**：添加带有 `@Tool` 注解的方法来扩展功能
3. **数据库持久化**：将内存列表替换为数据库存储
4. **增强搜索能力**：实现模糊搜索、标签过滤等高级功能

### 示例：添加搜索功能

```java
@Tool(name = "dv_search_courses", description = "Search courses containing a keyword")
public List<Course> searchCourses(String keyword) {
    return courses.stream()
            .filter(course -> course.title().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
}
```

## 与 AI 模型集成

### 小智 AI 集成

本项目主要用于与小智 AI 进行集成，配置步骤：

1. 在 `application.properties` 中配置小智 AI 的 WebSocket 端点
2. 配置百度地图 API Key（如需使用地图功能）
3. 启动应用，应用会通过 STDIO 传输与小智 AI 建立连接
4. AI 可以调用已注册的工具来获取课程信息、查询地图等

## 可用工具列表

运行后，服务器会注册以下工具供 AI 调用：

- **dv_get_courses**: 获取所有可用课程列表
- **dv_get_course**: 根据标题获取特定课程信息
- **百度地图相关工具**: 地理位置查询、路径规划等（需配置 API Key）
- **日历管理工具**: 创建和管理日历事件（Mac 系统）

## 总结

本项目提供了一个简洁、可扩展的框架，用于通过 Model Control Protocol 暴露多种服务能力。通过遵循 Spring AI 的约定和使用工具注解系统，可以轻松创建 AI 模型与数据服务之间的强大集成。

项目展示了如何在保持良好软件设计实践的同时构建 MCP 兼容的代码。基于这个基础，可以构建更复杂的数据提供者，为 AI 提供访问自定义领域特定信息的能力。

## 参考资源

- [Spring AI 官方文档](https://spring.io/projects/spring-ai)
- [小智 AI 控制台](https://xiaozhi.me/console)
- [百度地图开放平台](https://lbsyun.baidu.com/)
- [原始项目 dv-courses-mcp](https://github.com/danvega/dv-courses-mcp)

## 许可证

本项目基于原 dv-courses-mcp 项目改造，遵循相应的开源许可证。

