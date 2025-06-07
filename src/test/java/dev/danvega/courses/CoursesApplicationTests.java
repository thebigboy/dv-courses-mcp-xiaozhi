package dev.danvega.courses;

import com.alibaba.fastjson.JSONObject;
import dev.danvega.courses.service.BaiduMapService;
import dev.danvega.courses.service.CalenderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Map;

@SpringBootTest
class CoursesApplicationTests {

	@Autowired
	private BaiduMapService baiduMapService;


	@Autowired
	private CalenderService calenderService;


	@Test
	void addCalendarEvent() throws IOException {

		Map<String,Object> result = calenderService.addCalendarEvent("上课","学校","2025-06-07 15:30:00","2025-06-07 16:30:30");
		System.out.println("添加日程结果如下："+ JSONObject.toJSONString(result));
	}

	@Test
	void getMacCalendarEventsByDate() throws IOException {

		String result = calenderService.getMacCalendarEventsByDate("2025-06-07");
		System.out.println("行程如下："+result);
	}

	@Test
	void contextLoads() {
		Mono<String> location = baiduMapService.mapIpLocation(null);
		System.out.println(location.block());
	}

	@Test
	void mapDirections() {
		Mono<String> location = baiduMapService.mapDirections("南京南站","玄武湖","driving");
		System.out.println(location.block());
	}

	@Test
	void mapSearchPlaces() {
		Mono<String> location = baiduMapService.mapSearchPlaces("南京农业大小附属实验小学","","","",null);
		System.out.println(location.block());
	}

	@Test
	void mapWeather() {
		Mono<String> location = baiduMapService.mapWeather("南京",null);
		System.out.println(location.block());
	}


}
