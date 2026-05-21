package com.untitled.mapper;

import com.untitled.model.AdminCredential;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AdminCredentialMapper {
    @Select("select id, username, password, created_at, updated_at from admin_credentials order by id asc limit 1")
    AdminCredential findFirst();

    @Select("select id, username, password, created_at, updated_at from admin_credentials where id = #{id}")
    AdminCredential findById(long id);

    @Insert("insert into admin_credentials (id, username, password, created_at, updated_at) " +
            "values (#{id}, #{username}, #{password}, now(), now())")
    int insert(AdminCredential credential);

    @Update("update admin_credentials set username = #{username}, password = #{password}, updated_at = now() where id = #{id}")
    int update(AdminCredential credential);
}
