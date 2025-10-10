package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntRobotInfoRecord;
import com.example.WCS_DataStream.etl.service.EtlOffsetStore;
import com.example.WCS_DataStream.etl.service.SystemAgvRepository;
import com.example.WCS_DataStream.etl.service.WcsAntRobotRepository;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class AntRobotEtlEngine extends ETLEngine<AntRobotInfoRecord> {

    private final WcsAntRobotRepository wcs;
    private final SystemAgvRepository systemRepo;
    private final EtlOffsetStore offsetStore;

    private static final String JOB = "etl-ant-robot";

    public AntRobotEtlEngine(WcsAntRobotRepository wcs, SystemAgvRepository systemRepo, EtlOffsetStore offsetStore) {
        this.wcs = wcs;
        this.systemRepo = systemRepo;
        this.offsetStore = offsetStore;
    }

    @Override
    protected boolean checkTableExists() {
        return systemRepo.isConnected();
    }

    @Override
    public boolean isConnected() {
        return systemRepo.isConnected();
    }

    @Override
    protected List<AntRobotInfoRecord> extractData() throws ETLEngineException {
        EtlOffsetStore.Offset off = offsetStore.get(JOB);
        Timestamp lastTs = (off == null || off.lastTs == null) ? new Timestamp(0) : off.lastTs;
        String lastUuid = (off == null) ? null : off.lastUuid;
        List<AntRobotInfoRecord> rows = wcs.fetchIncremental(lastTs, lastUuid, 1000);
        updatePullTime();
        return rows;
    }

    @Override
    protected List<AntRobotInfoRecord> transformAndLoad(List<AntRobotInfoRecord> data) throws ETLEngineException {
        if (data == null || data.isEmpty()) return List.of();
        List<AntRobotInfoRecord> written = new ArrayList<>(data.size());
        Timestamp maxTs = null; String maxUuid = null;
        for (AntRobotInfoRecord r : data) {
            systemRepo.upsertAntRobotInfo(r);
            written.add(r);
            if (r.getUpdDt() != null) {
                if (maxTs == null || r.getUpdDt().after(maxTs) || (r.getUpdDt().equals(maxTs) && compareUuid(r.getUuid(), maxUuid) > 0)) {
                    maxTs = r.getUpdDt();
                    maxUuid = r.getUuid();
                }
            } else if (r.getInsDt() != null) {
                if (maxTs == null || r.getInsDt().after(maxTs) || (r.getInsDt().equals(maxTs) && compareUuid(r.getUuid(), maxUuid) > 0)) {
                    maxTs = r.getInsDt();
                    maxUuid = r.getUuid();
                }
            }
        }
        if (maxTs != null) {
            offsetStore.set(JOB, new EtlOffsetStore.Offset(maxTs, maxUuid));
        }
        return written;
    }

    private int compareUuid(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    @Override
    protected String getDataKey(AntRobotInfoRecord data) {
        return data.getUuid();
    }

    @Override
    protected boolean isSameData(AntRobotInfoRecord d1, AntRobotInfoRecord d2) {
        if (d1 == null || d2 == null) return false;
        return d1.getUuid() != null && d1.getUuid().equals(d2.getUuid());
    }

    @Override
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) {
        super.initialize(config, postgreSQLDataService);
    }
} 