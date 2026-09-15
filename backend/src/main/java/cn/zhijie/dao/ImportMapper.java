package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface ImportMapper {
    void insertBatch(InsertBatchCommand p);
    ImportBatchEntity batchByKey(BatchByKeyCommand p);
    ImportBatchEntity batch(UUID id);
    ImportBatchEntity lockBatch(UUID id);
    List<ImportBatchEntity> batches(UUID id);
    void finishBatch(FinishBatchCommand p);
    void enqueueBatch(EnqueueBatchCommand p);
    List<ImportBatchEntity> queuedBatches();
    List<ImportBatchEntity> batchesPage(@Param("id") UUID id, @Param("query") PageQuery query);
    long batchesCount(@Param("id") UUID id, @Param("query") PageQuery query);
}
