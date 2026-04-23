package com.smartfarm.shared.application;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 天气联动服务 — 对接 Open-Meteo 免费 API
 * 根据天气预报自动调整灌溉策略：下雨减少/跳过灌溉，高温增加灌溉
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 默认坐标（山东寿光，中国蔬菜之乡）— 可通过配置覆盖
    private double latitude = 36.86;
    private double longitude = 118.73;

    private volatile WeatherData currentWeather;
    private volatile WeatherForecast forecast;

    @Data
    public static class WeatherData {
        private double temperature;       // 当前温度 °C
        private double humidity;          // 相对湿度 %
        private double precipitation;     // 当前降水量 mm
        private int weatherCode;          // WMO 天气代码
        private boolean isRaining;        // 是否正在下雨
        private LocalDateTime updatedAt;
    }

    @Data
    public static class WeatherForecast {
        private double precipitationNext6h;   // 未来6小时累计降水 mm
        private double precipitationNext24h;  // 未来24小时累计降水 mm
        private double maxTempToday;          // 今日最高温
        private double minTempToday;          // 今日最低温
        private boolean willRainSoon;         // 未来6小时是否有雨
        private LocalDateTime updatedAt;
    }

    /**
     * 每30分钟刷新天气数据
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 5000)
    public void refreshWeather() {
        try {
            String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f" +
                "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code" +
                "&hourly=precipitation,temperature_2m" +
                "&forecast_days=1&timezone=Asia%%2FShanghai",
                latitude, longitude);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return;

            // 解析当前天气
            @SuppressWarnings("unchecked")
            Map<String, Object> current = (Map<String, Object>) resp.get("current");
            if (current != null) {
                WeatherData wd = new WeatherData();
                wd.setTemperature(toDouble(current.get("temperature_2m")));
                wd.setHumidity(toDouble(current.get("relative_humidity_2m")));
                wd.setPrecipitation(toDouble(current.get("precipitation")));
                wd.setWeatherCode(toInt(current.get("weather_code")));
                wd.setRaining(wd.getPrecipitation() > 0 || isRainCode(wd.getWeatherCode()));
                wd.setUpdatedAt(LocalDateTime.now());
                this.currentWeather = wd;
            }

            // 解析逐小时预报
            @SuppressWarnings("unchecked")
            Map<String, Object> hourly = (Map<String, Object>) resp.get("hourly");
            if (hourly != null) {
                @SuppressWarnings("unchecked")
                java.util.List<Number> precips = (java.util.List<Number>) hourly.get("precipitation");
                @SuppressWarnings("unchecked")
                java.util.List<Number> temps = (java.util.List<Number>) hourly.get("temperature_2m");

                WeatherForecast wf = new WeatherForecast();
                double sum6h = 0, sum24h = 0;
                double maxTemp = -999, minTemp = 999;
                if (precips != null) {
                    for (int i = 0; i < Math.min(precips.size(), 24); i++) {
                        double p = precips.get(i).doubleValue();
                        sum24h += p;
                        if (i < 6) sum6h += p;
                    }
                }
                if (temps != null) {
                    for (int i = 0; i < Math.min(temps.size(), 24); i++) {
                        double t = temps.get(i).doubleValue();
                        maxTemp = Math.max(maxTemp, t);
                        minTemp = Math.min(minTemp, t);
                    }
                }
                wf.setPrecipitationNext6h(sum6h);
                wf.setPrecipitationNext24h(sum24h);
                wf.setMaxTempToday(maxTemp);
                wf.setMinTempToday(minTemp);
                wf.setWillRainSoon(sum6h > 1.0);
                wf.setUpdatedAt(LocalDateTime.now());
                this.forecast = wf;
            }

            log.info("天气更新: temp={}°C, humidity={}%, precip={}mm, raining={}, forecast6h={}mm",
                    currentWeather.getTemperature(), currentWeather.getHumidity(),
                    currentWeather.getPrecipitation(), currentWeather.isRaining(),
                    forecast != null ? forecast.getPrecipitationNext6h() : "N/A");

        } catch (Exception e) {
            log.warn("天气数据获取失败: {}", e.getMessage());
        }
    }

    public WeatherData getCurrentWeather() { return currentWeather; }
    public WeatherForecast getForecast() { return forecast; }

    /**
     * 计算天气对灌溉的调整系数
     * 返回 0.0 ~ 1.5：
     *   0.0 = 跳过灌溉（大雨）
     *   0.5 = 减半灌溉（小雨/即将下雨）
     *   1.0 = 正常灌溉
     *   1.2 = 增加灌溉（高温干燥）
     *   1.5 = 大幅增加（极端高温）
     */
    public double getIrrigationAdjustmentFactor() {
        if (currentWeather == null) return 1.0; // 无天气数据时不调整

        // 正在下大雨 → 跳过
        if (currentWeather.getPrecipitation() > 5.0) return 0.0;

        // 正在下小雨 → 减半
        if (currentWeather.isRaining()) return 0.5;

        // 未来6小时有雨 → 减少30%
        if (forecast != null && forecast.isWillRainSoon()) return 0.7;

        // 高温干燥 → 增加灌溉
        if (currentWeather.getTemperature() > 35 && currentWeather.getHumidity() < 40) return 1.5;
        if (currentWeather.getTemperature() > 32) return 1.2;

        return 1.0;
    }

    /**
     * 获取天气描述文本
     */
    public String getWeatherDescription() {
        if (currentWeather == null) return "天气数据暂不可用";
        String desc = String.format("%.1f°C, 湿度%.0f%%", currentWeather.getTemperature(), currentWeather.getHumidity());
        if (currentWeather.isRaining()) desc += ", 正在下雨";
        if (forecast != null && forecast.isWillRainSoon()) desc += ", 预计6小时内有雨";
        return desc;
    }

    private boolean isRainCode(int code) {
        // WMO weather codes: 51-67 drizzle/rain, 71-77 snow, 80-82 rain showers, 95-99 thunderstorm
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || (code >= 95 && code <= 99);
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    public void setCoordinates(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
    }
}
