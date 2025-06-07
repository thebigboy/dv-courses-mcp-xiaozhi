package dev.danvega.courses.mcp;

import ch.qos.logback.core.util.StringUtil;
import dev.danvega.courses.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;

/**
 * 连接小智AI聊天机器人的MCP，通过mcp服务来注册
 */
@Configuration
public class MCPClientConfig {
    private Logger logger = LoggerFactory.getLogger(MCPClientConfig.class);

    @Value("${endpoint:}")
    private String endpoint;

    @Bean
    public ApplicationRunner mcpClientRunner(CourseService courseService, List<ToolCallback> toolCallbacks) {
        return args -> {
            if(StringUtil.isNullOrEmpty(endpoint)) {
                throw new IllegalArgumentException("MCP client endpoint is required");
            }
            logger.info("MCP Client URL: {}", endpoint);
            logger.info("MCP Client init ...");
            MCPWebSocketClient client = new MCPWebSocketClient(new URI(endpoint), courseService, toolCallbacks);
            client.connectBlocking();
            new Thread(() -> {
                try {
                    while (client.isOpen()) {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException ignored) {
                    logger.warn("MCP Client is interrupted",ignored);
                }
            }).start();
            logger.info("MCP Client init success...");
        };
    }
} 