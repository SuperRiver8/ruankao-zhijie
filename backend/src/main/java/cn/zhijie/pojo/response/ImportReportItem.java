package cn.zhijie.pojo.response;

public record ImportReportItem(
    int index,
    String kind,
    String externalId,
    String status,
    String message
) {}
