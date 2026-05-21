package com.untitled.mapper;

import com.untitled.model.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CategoryMapper {
    @Select("select id, `key`, name, sort, status, created_at, updated_at " +
            "from categories order by sort asc, id asc")
    List<Category> findAll();

    @Select("select id, `key`, name, sort, status, created_at, updated_at " +
            "from categories where status = 'active' order by sort asc, id asc")
    List<Category> findActive();

    @Select("select id, `key`, name, sort, status, created_at, updated_at from categories where id = #{id}")
    Category findById(long id);

    @Select("select id, `key`, name, sort, status, created_at, updated_at from categories where `key` = #{key}")
    Category findByKey(String key);

    @Insert("insert into categories (`key`, name, sort, status, created_at, updated_at) " +
            "values (#{key}, #{name}, #{sort}, #{status}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Update("update categories set `key` = #{key}, name = #{name}, sort = #{sort}, status = #{status}, " +
            "updated_at = now() where id = #{id}")
    int update(Category category);

    @Delete("delete from categories where id = #{id}")
    int delete(long id);
}
