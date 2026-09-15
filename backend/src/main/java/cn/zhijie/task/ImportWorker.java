package cn.zhijie.task;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.ImportMapper;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.service.ImportService;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.jobs-enabled",havingValue="true",matchIfMissing=true)
public class ImportWorker {

    private final ImportMapper importMapper;
    private final ImportService service;

    public ImportWorker(ImportMapper importMapper, ImportService service) {
        this.importMapper = importMapper;
        this.service = service;
    }

    @Scheduled(fixedDelay = 3000)
    public void run() {
        for (var job : importMapper.queuedBatches()) {
            var id = uuid(job.getId());
            try {
                service.process(id);
            } catch (Exception e) {
                importMapper.finishBatch(
                    new FinishBatchCommand(
                        id,
                        "FAILED",
                        JSON.valueToTree(
                            map(
                                "error",
                                e instanceof
                                    org.springframework.web.server.ResponseStatusException r
                                    ? r.getReason()
                                    : "导入失败，事务已回滚；请重试或检查引用冲突"
                            )
                        )
                    )
                );
            }
        }
    }
}
