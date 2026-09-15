package cn.zhijie.dao;

import cn.zhijie.pojo.entity.AdminUserEntity;
import cn.zhijie.pojo.query.*;
import java.util.*;

public interface AdminUserMapper {
    AdminUserEntity user(UUID id);
    AdminUserEntity userByName(String username);
    AdminUserEntity lockUser(UUID id);
    List<AdminUserEntity> lockAccounts();
    List<AdminUserEntity> page(AdminQuery query);
    long count(AdminQuery query);
    void insert(SaveAdminCommand command);
    void update(SaveAdminCommand command);
    void updateAccount(UpdateAccountCommand command);
}
