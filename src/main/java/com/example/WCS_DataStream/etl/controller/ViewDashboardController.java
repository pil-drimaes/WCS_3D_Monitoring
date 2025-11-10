package com.example.WCS_DataStream.etl.controller;

import com.example.WCS_DataStream.etl.model.view.BatchViewRow;
import com.example.WCS_DataStream.etl.model.view.CapaDayViewRow;
import com.example.WCS_DataStream.etl.model.view.CapaHourViewRow;
import com.example.WCS_DataStream.etl.model.view.FloorRateViewRow;
import com.example.WCS_DataStream.etl.model.view.McStaMstViewRow;
import com.example.WCS_DataStream.etl.model.view.ZoneRateViewRow;
import com.example.WCS_DataStream.etl.service.WcsBatchViewRepository;
import com.example.WCS_DataStream.etl.service.WcsCapaDayViewRepository;
import com.example.WCS_DataStream.etl.service.WcsCapaHourViewRepository;
import com.example.WCS_DataStream.etl.service.WcsFloorRateViewRepository;
import com.example.WCS_DataStream.etl.service.WcsMcStaMstViewRepository;
import com.example.WCS_DataStream.etl.service.WcsZoneRateViewRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/view", produces = MediaType.APPLICATION_JSON_VALUE)
public class ViewDashboardController {

    private final WcsCapaHourViewRepository capaHourRepo;
    private final WcsCapaDayViewRepository capaDayRepo;
    private final WcsZoneRateViewRepository zoneRateRepo;
    private final WcsFloorRateViewRepository floorRateRepo;
    private final WcsBatchViewRepository batchRepo;
    private final WcsMcStaMstViewRepository mcRepo;

    public ViewDashboardController(WcsCapaHourViewRepository capaHourRepo,
                                   WcsCapaDayViewRepository capaDayRepo,
                                   WcsZoneRateViewRepository zoneRateRepo,
                                   WcsFloorRateViewRepository floorRateRepo,
                                   WcsBatchViewRepository batchRepo,
                                   WcsMcStaMstViewRepository mcRepo) {
        this.capaHourRepo = capaHourRepo;
        this.capaDayRepo = capaDayRepo;
        this.zoneRateRepo = zoneRateRepo;
        this.floorRateRepo = floorRateRepo;
        this.batchRepo = batchRepo;
        this.mcRepo = mcRepo;
    }

    @GetMapping("/capacity/hour")
    public Map<String, Object> getCapacityHour() {
        List<CapaHourViewRow> rows = capaHourRepo.fetchAll();
        Map<String, List<Map<String, Object>>> grouped = rows.stream()
            .collect(Collectors.groupingBy(
                r -> safeUpper(r.type()),
                Collectors.mapping(r -> point("t", r.time(), r.qty()), Collectors.toList())
            ));
        List<Map<String, Object>> series = new ArrayList<>();
        grouped.forEach((type, pts) -> {
            pts.sort(Comparator.comparing(p -> (String)p.get("t")));
            Map<String, Object> s = new HashMap<>();
            s.put("type", type);
            s.put("points", pts);
            series.add(s);
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("series", series);
        return resp;
    }

    @GetMapping("/capacity/day")
    public Map<String, Object> getCapacityDay() {
        List<CapaDayViewRow> rows = capaDayRepo.fetchAll();
        Map<String, List<Map<String, Object>>> grouped = rows.stream()
            .collect(Collectors.groupingBy(
                r -> safeUpper(r.type()),
                Collectors.mapping(r -> point("d", r.time(), r.qty()), Collectors.toList())
            ));
        List<Map<String, Object>> series = new ArrayList<>();
        grouped.forEach((type, pts) -> {
            pts.sort(Comparator.comparing(p -> (String)p.get("d")));
            Map<String, Object> s = new HashMap<>();
            s.put("type", type);
            s.put("points", pts);
            series.add(s);
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("series", series);
        return resp;
    }

    @GetMapping("/zone/rate")
    public Map<String, Object> getZoneRate() {
        List<ZoneRateViewRow> rows = zoneRateRepo.fetchAll();
        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("zone", r.zoneCd());
            m.put("rate", r.rate());
            m.put("qty", r.qty());
            return m;
        }).toList();
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        return resp;
    }

    @GetMapping("/floor/rate")
    public Map<String, Object> getFloorRate() {
        List<FloorRateViewRow> rows = floorRateRepo.fetchAll();
        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("zone", r.zoneCd());
            m.put("floor", r.floor());
            m.put("rate", r.rate());
            m.put("qty", r.qty());
            return m;
        }).toList();
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        return resp;
    }

    @GetMapping("/batch")
    public Map<String, Object> getBatch() {
        List<BatchViewRow> rows = batchRepo.fetchAll();
        LocalTime now = LocalTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.US);
        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.batchNo());
            m.put("start", r.startTime());
            m.put("end", r.endTime());
            try {
                LocalTime s = LocalTime.parse(r.startTime(), fmt);
                LocalTime e = LocalTime.parse(r.endTime(), fmt);
                double progress = computeProgress(now, s, e);
                m.put("progress", progress);
                m.put("status", progress >= 1.0 ? "완료" : (progress <= 0 ? "대기" : "진행중"));
            } catch (Exception ignore) {
                m.put("progress", 0.0);
                m.put("status", "알수없음");
            }
            return m;
        }).toList();
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        return resp;
    }

    @GetMapping("/mc/status")
    public Map<String, Object> getMcStatus() {
        List<McStaMstViewRow> rows = mcRepo.fetchAll();
        Map<String, Map<String, Integer>> agg = new HashMap<>();
        for (McStaMstViewRow r : rows) {
            String key = safeUpper(r.mcTyp());
            Map<String, Integer> s = agg.computeIfAbsent(key, k -> new HashMap<>());
            boolean isError = r.errorCode() != null && !r.errorCode().isBlank();
            String bucket = isError ? "error" : "normal"; // 경고 구분 규칙 없어서 0 처리
            s.put(bucket, s.getOrDefault(bucket, 0) + 1);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        agg.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("type", k);
            m.put("normal", v.getOrDefault("normal", 0));
            m.put("warning", v.getOrDefault("warning", 0));
            m.put("error", v.getOrDefault("error", 0));
            items.add(m);
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        return resp;
    }

    private static Map<String, Object> point(String key, String x, java.math.BigDecimal y) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, x);
        m.put("y", y);
        return m;
    }

    private static String safeUpper(String s) { return s == null ? "" : s.toUpperCase(Locale.ROOT); }

    private static double computeProgress(LocalTime now, LocalTime start, LocalTime end) {
        // 배치가 자정을 넘어가는 케이스(예: 22:00~02:00) 처리
        long startSec = start.toSecondOfDay();
        long endSec = end.toSecondOfDay();
        long nowSec = now.toSecondOfDay();
        if (endSec <= startSec) endSec += 24 * 3600;
        if (nowSec < startSec) nowSec += 24 * 3600;
        double total = Math.max(1, endSec - startSec);
        double done = nowSec - startSec;
        double p = done / total;
        if (p < 0) return 0.0; if (p > 1) return 1.0; return p;
    }
}


