package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    void updateAccount(UpdateAccountCommand command);
    UserEntity userByName(String username);
    UserEntity user(UUID id);
    UserEntity lockUser(UUID id);
    void insertUser(InsertUserCommand p);
    List<UserEntity> users();
    void updateUser(UpdateUserCommand p);
    void verifyIdentity(VerifyIdentityCommand p);
    List<UserEntity> usersPage(@Param("query") PageQuery query);
    long usersCount(@Param("query") PageQuery query);
}
