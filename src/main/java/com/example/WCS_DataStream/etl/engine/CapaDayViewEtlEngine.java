package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.view.CapaDayViewRow;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.service.SystemCapaDayViewRepository;
import com.example.WCS_DataStream.etl.service.WcsCapaDayViewRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CapaDayViewEtlEngine extends ETLEngine<CapaDayViewRow> {

    private final WcsCapaDayViewRepository wcs;
    private final SystemCapaDayViewRepository systemRepo;

    public CapaDayViewEtlEngine(WcsCapaDayViewRepository wcs, SystemCapaDayViewRepository systemRepo) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
    }

    @Override
    protected boolean checkTableExists() { return systemRepo.isConnected(); }

    @Override
    public boolean isConnected() { return systemRepo.isConnected(); }

    @Override
    protected List<CapaDayViewRow> extractData() throws ETLEngineException {
        return wcs.fetchAll();
    }

    @Override
    protected List<CapaDayViewRow> transformAndLoad(List<CapaDayViewRow> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        LocalDate workDate = LocalDate.now();
        // 날짜 기준 최종값 보존: 동일 날짜 단일 행(TIME='TOTAL')로 upsert
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (CapaDayViewRow r : data) {
            if (r.qty() != null) total = total.add(r.qty());
        }
        systemRepo.upsert(workDate, "TOTAL", total, null);
        return data;
    }

    @Override
    protected String getDataKey(CapaDayViewRow data) { return (data == null ? null : data.time()); }

    @Override
    protected boolean isSameData(CapaDayViewRow d1, CapaDayViewRow d2) { return false; }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) { super.initialize(config, postgreSQLDataService); }
}


