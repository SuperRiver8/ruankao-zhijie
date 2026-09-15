package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface ContentMapper {
    List<ContentProjection> contents(
        @Param("kind") String kind,
        @Param("published") boolean published
    );
    ContentProjection content(UUID id);
    ContentProjection publishedContent(UUID id);
    ContentProjection findContent(FindContentCommand p);
    void insertEntity(InsertEntityCommand p);
    void nextVersion(NextVersionCommand p);
    void insertVersion(InsertVersionCommand p);
    int publish(PublishCommand p);
    List<ContentProjection> contentsPage(
        @Param("kind") String kind,
        @Param("published") boolean published,
        @Param("query") PageQuery query
    );
    long contentsCount(
        @Param("kind") String kind,
        @Param("published") boolean published,
        @Param("query") PageQuery query
    );
}
