package dev.danvega.courses;

import dev.danvega.courses.service.BaiduMapService;
import dev.danvega.courses.service.CalenderService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class CourseService {

    @Autowired
    private CalenderService calenderService;

    @Autowired
    private BaiduMapService baiduMapService;

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);
    private List<Course> courses = new ArrayList<>();

    @Tool(name = "dv_get_courses", description = "获取全部的课程信息")
    public List<Course> getCourses() {
        return courses;
    }

    /**
     * 获取课程名称获取课程的完整信息
     * @param title 课程标题
     * @return
     */
    @Tool(name = "dv_get_course", description = "获取课程名称获取课程的完整信息")
    public Course getCourse(String title) {
        return courses.stream().filter(course -> course.title().equals(title)).findFirst().orElse(null);
    }

    /**
     * 获取某一天的日程信息
     * @param date 某一天的日期，格式：yyyy-MM-dd
     * @return str
     */
    @Tool(name = "getCalenderForMac", description = "获取某一天的日程信息")
    public String getCalenderForMac(@ToolParam(required = true,description = "某一天的日期，格式：yyyy-MM-dd")String date) {
        //校验date的格式
        log.info("getCalenderForMac-input:{}", date);
        try{
            String result =  calenderService.getMacCalendarEventsByDate(date);
            log.info("getCalenderForMac-output:{}", result);
            return result;
        }catch (Exception e){
            log.error("getCalenderForMac:{}", date, e);
            return "获取日程信息失败";
        }
    }

    @Tool(name = "addCalendarEvent", description = "添加一项日程，如果没有提及结束时间，则默认将开始时间加半个小时")
    public Map<String,Object> addCalendarEvent(
            @ToolParam(required = true,description = "事项的名称")String title,
            @ToolParam(required = true,description = "地点，如果没有提及，则给空值")String location,
            @ToolParam(required = true,description = "开始时间，格式：yyyy-MM-dd HH:mm:ss，例如")String startTime,
            @ToolParam(required = true,description = "结束时间，格式：yyyy-MM-dd HH:mm:ss")String endTime
        ) {
        //校验date的格式
        log.info("addCalendarEvent-input:{}", Arrays.asList(title,location,startTime,endTime));
        try{
            Map<String,Object> result =  calenderService.addCalendarEvent(title,location,startTime,endTime);
            log.info("addCalendarEvent-output:{}", result);
            return result;
        }catch (Exception e){
            log.error("addCalendarEvent:{}", e);
            return HashMap.newHashMap(1);
        }
    }

    // --- 百度地图工具方法 ---

    @Tool(name = "map_geocode", description = "地理编码服务: 将地址解析为对应的位置坐标.地址结构越完整, 地址内容越准确, 解析的坐标精度越高.")
    public String mapGeocode(@ToolParam(required = true, description = "待解析的地址.最多支持84个字节.可以输入两种样式的值, 分别是：\n1、标准的结构化地址信息, 如北京市海淀区上地十街十号\n2、支持*路与*路交叉口描述方式, 如北一环路和阜阳路的交叉路口\n第二种方式并不总是有返回结果, 只有当地址库中存在该地址描述时才有返回") String address) {
        return baiduMapService.mapGeocode(address).block();
    }

    @Tool(name = "map_reverse_geocode", description = "逆地理编码服务: 根据纬经度坐标, 获取对应位置的地址描述, 所在行政区划, 道路以及相关POI等信息")
    public String mapReverseGeocode(
            @ToolParam(required = true, description = "纬度 (bd09ll)") double latitude,
            @ToolParam(required = true, description = "经度 (bd09ll)") double longitude) {
        return baiduMapService.mapReverseGeocode(latitude, longitude).block();
    }

    @Tool(name = "map_search_places", description = "地点检索服务: 支持检索城市内的地点信息(最小到city级别), 也可支持圆形区域内的周边地点信息检索.\n城市内检索: 检索某一城市内（目前最细到城市级别）的地点信息.\n周边检索: 设置圆心和半径, 检索圆形区域内的地点信息（常用于周边检索场景）.")
    public String mapSearchPlaces(
            @ToolParam(required = true, description = "检索关键字, 可直接使用名称或类型, 如'天安门', 且可以至多10个关键字, 用英文逗号隔开") String query,
            @ToolParam(description = "检索分类, 以中文字符输入, 如'美食', 多个分类用英文逗号隔开, 如'美食,购物'") String tag,
            @ToolParam(description = "检索的城市名称, 可为行政区划名或citycode, 格式如'北京市'或'131', 不传默认为'全国'") String region,
            @ToolParam(description = "圆形区域检索的中心点纬经度坐标, 格式为lat,lng") String location,
            @ToolParam(description = "圆形区域检索半径, 单位：米") Integer radius) {
        return baiduMapService.mapSearchPlaces(query, tag, region, location, radius).block();
    }

//     @Tool(name = "map_place_details", description = "地点详情检索服务: 地点详情检索针对指定POI, 检索其相关的详情信息.\n通过地点检索服务获取POI uid.使用地点详情检索功能, 传入uid, 即可检索POI详情信息, 如评分、营业时间等(不同类型POI对应不同类别详情数据).")
//    public String mapPlaceDetails(@ToolParam(required = true, description = "POI的唯一标识") String uid) {
//        return baiduMapService.mapPlaceDetails(uid).block();
//    }

    @Tool(name = "map_directions_matrix", description = "批量算路服务: 根据起点和终点坐标计算路线规划距离和行驶时间.\n批量算路目前支持驾车、骑行、步行.\n步行时任意起终点之间的距离不得超过200KM, 超过此限制会返回参数错误.\n驾车批量算路一次最多计算100条路线, 起终点个数之积不能超过100.")
    public String mapDirectionsMatrix(
            @ToolParam(required = true, description = "多个起点纬经度坐标, 纬度在前, 经度在后, 多个起点用|分隔") String origins,
            @ToolParam(required = true, description = "多个终点纬经度坐标, 纬度在前, 经度在后, 多个终点用|分隔") String destinations,
            @ToolParam(description = "批量算路类型(driving, riding, walking)") String model) {
        return baiduMapService.mapDirectionsMatrix(origins, destinations, model).block();
    }

    @Tool(name = "map_directions", description = "路线规划服务: 根据起终点`位置名称`或`纬经度坐标`规划出行路线.\n驾车路线规划: 根据起终点`位置名称`或`纬经度坐标`规划驾车出行路线.\n骑行路线规划: 根据起终点`位置名称`或`纬经度坐标`规划骑行出行路线.\n步行路线规划: 根据起终点`位置名称`或`纬经度坐标`规划步行出行路线.\n公交路线规划: 根据起终点`位置名称`或`纬经度坐标`规划公共交通出行路线.")
    public String mapDirections(
            @ToolParam(required = true, description = "起点位置名称或纬经度坐标, 纬度在前, 经度在后") String origin,
            @ToolParam(required = true, description = "终点位置名称或纬经度坐标, 纬度在前, 经度在后") String destination,
            @ToolParam(description = "路线规划类型(driving, riding, walking, transit)") String model) {
        return baiduMapService.mapDirections(origin, destination, model).block();
    }

//     @Tool(name = "map_weather", description = "天气查询服务: 通过行政区划或是经纬度坐标查询实时天气信息及未来5天天气预报.")
//    public String mapWeather(
//             @ToolParam(description = "经纬度坐标, 经度在前纬度在后, 逗号分隔") String location,
//             @ToolParam(description = "行政区划代码, 需保证为6位无符号整数") Integer district_id) {
//        return baiduMapService.mapWeather(location, district_id).block();
//    }

//    @Tool(name = "map_ip_location", description = "IP定位服务: 通过所给IP获取具体位置信息和城市名称, 可用于定位IP或用户当前位置.")
//    public String mapIpLocation(@ToolParam(description = "需要定位的IP地址, 如果为空则获取用户当前IP地址(支持IPv4和IPv6)") String ip) {
//        return baiduMapService.mapIpLocation(ip);
//    }

    @Tool(name = "getCurrentLocation", description = "根据当前的网络获取定位")
    public String getCurrentLocation(@ToolParam(description = "根据当前的网络获取定位") String ip) {
        return baiduMapService.mapIpLocation(ip).block();
    }

     @Tool(name = "map_road_traffic", description = "实时路况查询服务: 查询实时交通拥堵情况, 可通过指定道路名和区域形状(矩形, 多边形, 圆形)进行实时路况查询.\n道路实时路况查询: 查询具体道路的实时拥堵评价和拥堵路段、拥堵距离、拥堵趋势等信息.\n矩形区域实时路况查询: 查询指定矩形地理范围的实时拥堵情况和各拥堵路段信息.\n多边形区域实时路况查询: 查询指定多边形地理范围的实时拥堵情况和各拥堵路段信息.\n圆形区域(周边)实时路况查询: 查询某中心点周边半径范围内的实时拥堵情况和各拥堵路段信息.")
     public String mapRoadTraffic(
             @ToolParam(required = true, description = "路况查询类型(road, bound, polygon, around)") String model,
             @ToolParam(description = "道路名称和道路方向, model=road时必传 (如:朝阳路南向北)") String road_name,
             @ToolParam(description = "城市名称或城市adcode, model=road时必传 (如:北京市)") String city,
             @ToolParam(description = "区域左下角和右上角的纬经度坐标, 纬度在前, 经度在后, model=bound时必传") String bounds,
             @ToolParam(description = "多边形区域的顶点纬经度坐标, 纬度在前, 经度在后, model=polygon时必传") String vertexes,
             @ToolParam(description = "圆形区域的中心点纬经度坐标, 纬度在前, 经度在后, model=around时必传") String center,
             @ToolParam(description = "圆形区域的半径(米), 取值[1,1000], model=around时必传") Integer radius) {
        return baiduMapService.mapRoadTraffic(model, road_name, city, bounds, vertexes, center, radius).block();
    }

    @Tool(name = "map_poi_extract", description = "POI智能提取")
    public String mapPoiExtract(@ToolParam(required = true, description = "根据用户提供的文本描述信息, 智能提取出文本中所提及的POI相关信息. (注意: 使用该服务, api_key需要拥有对应的高级权限, 否则会报错)") String text_content) {
        return baiduMapService.mapPoiExtract(text_content).block()  ;
    }

    @PostConstruct
    public void init() {
        courses.addAll(Arrays.asList(
                new Course("计算机网络", "https://youtu.be/31KTdfRH6nY"),
                new Course("几何代数","https://youtu.be/UgX5lgv4uVM")
        ));
    }
}
