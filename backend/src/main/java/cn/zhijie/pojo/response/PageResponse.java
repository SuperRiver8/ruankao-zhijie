package cn.zhijie.pojo.response;

import cn.zhijie.pojo.query.PageQuery;
import java.util.List;

public record PageResponse<T>(List<T> records, long total, int page, int size) {
    public static <T> PageResponse<T> of(List<T> records, long total, PageQuery query) {
        return new PageResponse<>(records, total, query.getPage(), query.getSize());
    }
}
