package cn.zhijie.integration;

// 后续授权核验服务的边界；首期不注册自动官方核验实现。
public interface OfficialVerificationProvider {
    record Result(String status, String reference, String evidence) {}
    Result verify(String holder, String numberType, String number);
}
