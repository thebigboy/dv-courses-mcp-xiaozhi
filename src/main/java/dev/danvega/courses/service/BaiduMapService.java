package dev.danvega.courses.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class BaiduMapService {

    private static final Logger log = LoggerFactory.getLogger(BaiduMapService.class);

    @Value("${baidu.map.api.key}")
    private String apiKey;

    private final WebClient webClient;

    private final String apiUrl = "https://api.map.baidu.com";

    public BaiduMapService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(apiUrl).build();
    }

    /**
     * 地理编码服务: 将地址解析为对应的位置坐标.
     */
    public Mono<String> mapGeocode(String address) {
        log.info("mapGeocode-input: {}", address);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/geocoding/v3/")
                        .queryParam("ak", apiKey)
                        .queryParam("output", "json")
                        .queryParam("address", address)
                        .queryParam("from", "java_mcp")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> log.info("mapGeocode-output: {}", result));
    }

    /**
     * 逆地理编码服务: 根据纬经度坐标获取对应位置的地址描述.
     */
    public Mono<String> mapReverseGeocode(double latitude, double longitude) {
         log.info("mapReverseGeocode-input: {},{}", latitude, longitude);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/reverse_geocoding/v3/")
                        .queryParam("ak", apiKey)
                        .queryParam("output", "json")
                        .queryParam("location", latitude + "," + longitude)
                         .queryParam("extensions_road", "true")
                        .queryParam("extensions_poi", "1")
                        .queryParam("entire_poi", "1")
                        .queryParam("from", "java_mcp")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> log.info("mapReverseGeocode-output: {}", result));
    }

    /**
     * 地点检索服务: 支持检索城市内的地点信息或圆形区域内的周边地点信息.
     */
    public Mono<String> mapSearchPlaces(String query, String tag, String region, String location, Integer radius) {
         log.info("mapSearchPlaces-input: query={}, tag={}, region={}, location={}, radius={}", query, tag, region, location, radius);
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/place/v2/search")
                            .queryParam("ak", apiKey)
                            .queryParam("output", "json")
                            .queryParam("query", query)
                             .queryParam("tag", tag)
                            .queryParam("photo_show", "true")
                            .queryParam("scope", 2)
                             .queryParam("from", "java_mcp");
                    if (location != null && !location.isEmpty()) {
                        uriBuilder.queryParam("location", location);
                        if (radius != null) {
                            uriBuilder.queryParam("radius", radius);
                        }
                    } else if (region != null && !region.isEmpty()) {
                         uriBuilder.queryParam("region", region);
                    }
                     return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> log.info("mapSearchPlaces-output: {}", result));
    }

     /**
     * 地点详情检索服务, 获取指定POI的详情信息.
     */
    public Mono<String> mapPlaceDetails(String uid) {
        log.info("mapPlaceDetails-input: {}", uid);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/place/v2/detail")
                        .queryParam("ak", apiKey)
                        .queryParam("output", "json")
                        .queryParam("uid", uid)
                         .queryParam("scope", 2)
                        .queryParam("from", "java_mcp")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> log.info("mapPlaceDetails-output: {}", result));
    }

    /**
     * 批量算路服务: 根据起点和终点坐标计算路线规划距离和行驶时间.
     */
     public Mono<String> mapDirectionsMatrix(String origins, String destinations, String model) {
        log.info("mapDirectionsMatrix-input: origins={}, destinations={}, model={}", origins, destinations, model);
         String path = String.format("/routematrix/v2/%s", model != null ? model : "driving");
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("ak", apiKey)
                        .queryParam("output", "json")
                        .queryParam("origins", origins)
                        .queryParam("destinations", destinations)
                        .queryParam("from", "java_mcp")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> log.info("mapDirectionsMatrix-output: {}", result));
    }

    /**
     * 路线规划服务, 支持驾车、骑行、步行和公交路线规划.
     * NOTE: 这个方法需要处理地址转经纬度的逻辑，复杂一点。
     */
    public Mono<String> mapDirections(String origin, String destination, String model) {
         log.info("mapDirections-input: origin={}, destination={}, model={}", origin, destination, model);
        String finalModel = model != null ? model : "driving";

        // 简化的地址转经纬度逻辑，生产环境应更健壮
        Mono<String> originMono = isLatLng(origin) ? Mono.just(origin) : geocodeAddress(origin);
        Mono<String> destinationMono = isLatLng(destination) ? Mono.just(destination) : geocodeAddress(destination);

        return Mono.zip(originMono, destinationMono)
                .flatMap(coords -> {
                    String originCoord = coords.getT1();
                    String destinationCoord = coords.getT2();

                    String path = String.format("/directionlite/v1/%s", finalModel);
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder.path(path)
                                    .queryParam("ak", apiKey)
                                    .queryParam("output", "json")
                                    .queryParam("origin", originCoord)
                                    .queryParam("destination", destinationCoord)
                                     .queryParam("from", "java_mcp")
                                    .build())
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::filterDirectionsResult)
                            .doOnNext(result -> log.info("mapDirections-output: {}", result));
                });
    }

    /**
     * 过滤路线规划结果，只保留必要的导航信息
     */
    private String filterDirectionsResult(String jsonResult) {
        JSONObject root = JSON.parseObject(jsonResult);
        if (root.containsKey("result")) {
            JSONObject result = root.getJSONObject("result");
            if (result.containsKey("routes")) {
                JSONArray routes = result.getJSONArray("routes");
                for (int i = 0; i < routes.size(); i++) {
                    JSONObject route = routes.getJSONObject(i);
                    if (route.containsKey("steps")) {
                        JSONArray steps = route.getJSONArray("steps");
                        JSONArray newSteps = new JSONArray();
                        for (int j = 0; j < steps.size(); j++) {
                            JSONObject step = steps.getJSONObject(j);
                            JSONObject newStep = new JSONObject();
                            newStep.put("instruction", step.getString("instruction"));
                            newSteps.add(newStep);
                        }
                        route.put("steps", newSteps);
                    }
                }
            }
        }
        return JSON.toJSONString(root);
    }

     /**
     * 天气查询服务, 查询实时天气信息及未来5天天气预报.
     */
    public Mono<String> mapWeather(String location, Integer district_id) {
        log.info("mapWeather-input: location={}, district_id={}", location, district_id);
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/weather/v1/")
                            .queryParam("ak", apiKey)
                            .queryParam("data_type", "all")
                            .queryParam("from", "java_mcp");
                    if (location != null && !location.isEmpty()) {
                        uriBuilder.queryParam("location", location);
                    } else if (district_id != null) {
                        uriBuilder.queryParam("district_id", district_id);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                 .doOnNext(result -> log.info("mapWeather-output: {}", result));
    }

    /**
     * IP定位服务, 通过所给IP获取具体位置信息和城市名称.
     */
     public Mono<String> mapIpLocation(String ip) {
         ip = null;
         if(StringUtils.isEmpty(ip)) {
            //http://api4.ipify.org/?format=json
//            String ipResponse =  webClient.get()
//                     .uri("http://api4.ipify.org/?format=json")
//                     .retrieve()
//                     .bodyToMono(String.class).block();
            try{
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.ipplus360.com/getIP"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("mapIpLocation-output: {}", response.body());
                JSONObject json =  JSONObject.parseObject(response.body());
                ip = json.getString("data");
            }catch (Exception e){
                log.error("mapIpLocation-output: ", e);
                return Mono.just("");
            }
         }

         log.info("mapIpLocation-input: {}", ip);
         String finalIp = ip;
         return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/location/ip")
                             .queryParam("ak", apiKey)
                             .queryParam("from", "java_mcp");
                    if (finalIp != null && !finalIp.isEmpty()) {
                        uriBuilder.queryParam("ip", finalIp);
                    }
                     return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> log.info("mapIpLocation-output: {}", result));
    }

    /**
     * 实时路况查询服务, 查询实时交通拥堵情况.
     */
    public Mono<String> mapRoadTraffic(String model, String road_name, String city, String bounds, String vertexes, String center, Integer radius) {
         log.info("mapRoadTraffic-input: model={}, road_name={}, city={}, bounds={}, vertexes={}, center={}, radius={}", model, road_name, city, bounds, vertexes, center, radius);
        String path = String.format("/traffic/v1/%s", model != null ? model : "");
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path)
                            .queryParam("ak", apiKey)
                            .queryParam("output", "json")
                            .queryParam("from", "java_mcp");
                    switch (model != null ? model : "") {
                        case "bound" -> uriBuilder.queryParam("bounds", bounds);
                        case "polygon" -> uriBuilder.queryParam("vertexes", vertexes);
                        case "around" -> {
                             uriBuilder.queryParam("center", center);
                             if(radius != null) uriBuilder.queryParam("radius", radius);
                        }
                        case "road" -> {
                            uriBuilder.queryParam("road_name", road_name);
                            uriBuilder.queryParam("city", city);
                        }
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                 .doOnNext(result -> log.info("mapRoadTraffic-output: {}", result));
    }

    /**
     * POI智能提取
     * NOTE: 这个方法需要实现提交和轮询结果的逻辑，复杂一点。
     */
    public Mono<String> mapPoiExtract(String text_content) {
         log.info("mapPoiExtract-input: {}", text_content);
        // 简化的POI提取逻辑，生产环境应实现提交和轮询
         return Mono.just("{\"result\": \"POI提取功能待实现\"}");
    }

    // --- Helper Methods ---

    private boolean isLatLng(String text) {
         if (text == null || text.trim().isEmpty()) return false;
        // 简单的经纬度格式校验
        return text.matches("^\\s*[+-]?\\d+(\\.\\d+)?\\s*,\\s*[+-]?\\d+(\\.\\d+)?\\s*$");
    }

    private Mono<String> geocodeAddress(String address) {
         log.info("Geocoding address: {}", address);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/geocoding/v3/")
                        .queryParam("ak", apiKey)
                        .queryParam("output", "json")
                        .queryParam("address", address)
                         .queryParam("from", "java_mcp_helper")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                 .flatMap(result -> {
                    JSONObject json = JSON.parseObject(result);
                     if (json.getIntValue("status") == 0) {
                        JSONObject location = json.getJSONObject("result").getJSONObject("location");
                        if (location != null) {
                             return Mono.just(location.getDoubleValue("lat") + "," + location.getDoubleValue("lng"));
                        }
                    }
                     log.error("Geocoding failed for {}: {}", address, result);
                    return Mono.error(new RuntimeException("Failed to geocode address: " + address));
                })
                .doOnError(e -> log.error("Geocoding error for {}: {}", address, e.getMessage()));
    }
} 