package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.ContentMapper;
import cn.zhijie.dao.ExamMapper;
import cn.zhijie.dao.LearningMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.ExamService;
import cn.zhijie.util.ResponseMapper;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class LearningService {

    private final ContentMapper contentMapper;

    private final LearningMapper learningMapper;

    public LearningService(ContentMapper contentMapper, LearningMapper learningMapper) {
        this.contentMapper = contentMapper;

        this.learningMapper = learningMapper;
    }

    public PageResponse<LearningResponse> learning(Actor a, PageQuery query) {
        return PageResponse.of(
            learningMapper
                .learningPage(a.id(), query)
                .stream()
                .map(ResponseMapper::learning)
                .toList(),
            learningMapper.learningCount(a.id(), query),
            query
        );
    }

    public void learning(Actor a, UUID id, LearningRequest p) {
        var q = found(contentMapper.publishedContent(id));
        check(p.favorite() instanceof Boolean, "favorite 必须是布尔值");
        int progress = p.progress() == null ? 0 : p.progress();
        check(progress >= 0 && progress <= 100, "进度范围为 0–100");
        var prior = learningMapper
            .learning(a.id())
            .stream()
            .filter(l -> l.getEntityId().equals(id))
            .findFirst()
            .orElseGet(LearningProjection::new);
        learningMapper.saveLearning(
            new SaveLearningCommand(
                a.id(),
                id,
                q.getVersion(),
                p.favorite(),
                Boolean.TRUE.equals(prior.getWrong()),
                progress
            )
        );
    }
}
