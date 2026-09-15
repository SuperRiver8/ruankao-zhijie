package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface ExamMapper {
    void insertAttempt(InsertAttemptCommand p);
    ExamEntity attempt(UUID id);
    ExamEntity lockAttempt(UUID id);
    void saveAnswers(SaveAnswersCommand p);
    void submitAttempt(SubmitAttemptCommand p);
    List<ExamEntity> attempts(UUID id);
    List<ExamEntity> attemptsPage(@Param("id") UUID id, @Param("query") PageQuery query);
    long attemptsCount(@Param("id") UUID id, @Param("query") PageQuery query);
}
