package dev.danvega.courses.mcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.danvega.courses.tools.Tool;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.danvega.courses.CourseService;

/**
 * 收到小智AI聊天机器人后的消息处理器
 * 格式如下：
 *  {"id":0,"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"sampling":{},"roots":{"listChanged":false}},"clientInfo":{"name":"xz-mcp-broker","version":"0.0.1"}}}
 */
public class MCPWebSocketClient extends WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger("MCP_PIPE");
    private final CourseService courseService;
    private final List<ToolCallback> toolCallbacks;

    public MCPWebSocketClient(URI serverUri, CourseService courseService, List<ToolCallback> toolCallbacks) {
        super(serverUri);
        this.courseService = courseService;
        this.toolCallbacks = toolCallbacks;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("WebSocket 连接已建立");
    }

    @Override
    public void onMessage(String message) {
        log.info("收到客户端请求: {}", message);
        try {
            JSONObject root = JSON.parseObject(message);
            String method = root.getString("method");
            Integer id = root.getInteger("id"); // Use getInteger to handle null safely
            JSONObject params = root.getJSONObject("params");

            String response = null;
            switch (method) {
                case "initialize" -> response = handleInitialize(id);
                case "tools/list" -> response = handleToolsList(id);
                case "ping" -> response = handlePing(id);
                case "notifications/initialized" -> {
                    // Notification, no response needed
                    return;
                }
                case "tools/call" -> response = handleToolExecute(id, params);
                default -> {
                    response = buildError(id, "Method not found: " + method);
                }
            }
            if (response != null) {
                log.info("返回给客户端: {}", response);
                send(response);
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            send(buildError(-1, "Error processing message: " + e.getMessage()));
        }
    }

    private String handleInitialize(Integer id) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("experimental", new HashMap<>());
        capabilities.put("prompts", Map.of("listChanged", false));
        capabilities.put("resources", Map.of("subscribe", false, "listChanged", false));
        capabilities.put("tools", Map.of("listChanged", false));
        result.put("capabilities", capabilities);
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "mcp-server-java-demo");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        result.put("instructions", "This is a MCP server for Java Demo, exposing CourseService and BaiduMapService tools.");
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("result", result);
        return JSON.toJSONString(resp);
    }

    private String handleToolsList(Integer id) {
        List<Tool> tools = new ArrayList<>();

        for (ToolCallback toolCallback : toolCallbacks) {

            // 直接使用 ToolCallback 的方法获取元数据
            Tool tool  = new Tool();  // = new HashMap<>();
//            tool.put("name", toolCallback.getName());
//            tool.put("description", toolCallback.getDescription());
            tool.setName(toolCallback.getName());
            tool.setDescription(toolCallback.getDescription());
            String inputSchema = toolCallback.getInputTypeSchema();
            JSONObject inputSchemaJson = JSONObject.parseObject(inputSchema);
            tool.setInputSchema(inputSchemaJson);
            // Spring AI 的 toolCallback 提供了 getInputSchema()

            tools.add(tool);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);

        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("result", result);
        return JSON.toJSONString(resp);
    }

    private String handlePing(Integer id) {
        log.info("Processing request of type PingRequest");
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("result", new HashMap<>());
        return JSON.toJSONString(resp);
    }

    /**
     * 返回内容的格式如下：
     * {
     *     "jsonrpc": "2.0",
     *     "id": 3,
     *     "result": {
     *         "content": [
     *             {
     *                 "type": "text",
     *                 "text": "{\n  \"success\": true,\n  \"result\": [\n    {\n      \"type\": \"text\",\n      \"text\": \"{\\\"status\\\":0,\\\"result\\\":{\\\"location\\\":{\\\"lng\\\":119.18096371060632,\\\"lat\\\":31.623829565701859},\\\"precise\\\":0,\\\"confidence\\\":70,\\\"comprehension\\\":0,\\\"level\\\":\\\"教育\\\"}}\",\n      \"annotations\": null\n    }\n  ]\n}"
     *             }
     *         ],
     *         "isError": false
     *     }
     * }
     * @param id
     * @param params
     * @return
     */
    private String handleToolExecute(Integer id, JSONObject params) {
        if (params == null || !params.containsKey("name") || !params.containsKey("arguments")) {
            return buildError(id, "Invalid tool/execute request");
        }

        String toolName = params.getString("name");
        // The arguments are sent as a JSON object, get the string representation
        String arguments = params.getJSONObject("arguments").toJSONString();

        try {
            // Find the corresponding ToolCallback and execute
            for (ToolCallback toolCallback : toolCallbacks) {
                if (toolCallback.getName().equals(toolName)) {
                    // The call method on ToolCallback expects a String argument in M6
                    Object toolResult = toolCallback.call(arguments);
                    log.info("业务执行返回了: {}", toolResult);

                    List<Map<String,String>> contentList = new ArrayList<>();
                    Map<String, String> contentMap = new HashMap<>();
                    contentMap.put("type", "text");
                    contentMap.put("text", toolResult.toString());
                    contentList.add(contentMap);

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("content", contentList);
                    resultMap.put("isError", false);

                    Map<String, Object> resp = new HashMap<>();
                    resp.put("jsonrpc", "2.0");
                    resp.put("id", id);
                    resp.put("result", resultMap);
                    return JSON.toJSONString(resp);

                }
            }
            return buildError(id, "Tool not found: " + toolName);

        } catch (Exception e) {
            log.error("Error executing tool {}", toolName, e);
            return buildError(id, "Error executing tool: " + e.getMessage());
        }
    }

    private String buildError(Integer id, String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        if (id != null && id != -1) { // Include id if it's valid
            resp.put("id", id);
        }
        Map<String, Object> error = new HashMap<>();
        // Use standard JSON-RPC error codes where applicable
        // -32601: Method not found
        // -32602: Invalid params
        // -32603: Internal error
        error.put("code", -32603); // Default to Internal error
        error.put("message", message);
        resp.put("error", error);
        return JSON.toJSONString(resp);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket connection closed: {}", reason);
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error", ex);
    }
}