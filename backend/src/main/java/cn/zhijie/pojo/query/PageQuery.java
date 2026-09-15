package cn.zhijie.pojo.query;

import jakarta.validation.constraints.*;

public class PageQuery {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    @Pattern(regexp = "id|time")
    private String sort = "time";

    @Pattern(regexp = "asc|desc")
    private String direction = "desc";

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public long getOffset() {
        return (long) (page - 1) * size;
    }
}
