package com.untitled.mapper;

import com.untitled.model.Club;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface ClubMapper {
    @Select("select id, name, location, address, phone, open_time, douyin_url, tags, cover, license_image, gallery, created_at, updated_at from clubs order by id")
    List<Club> findAll();

    @Select("select id, name, location, address, phone, open_time, douyin_url, tags, cover, license_image, gallery, created_at, updated_at from clubs where id = #{id}")
    Club findById(long id);

    @Select("select cover, license_image as licenseImage, gallery from clubs")
    List<Map<String, Object>> findAllImageRefs();

    @Insert("insert into clubs (name, location, address, phone, open_time, douyin_url, tags, cover, license_image, gallery, created_at, updated_at) " +
            "values (#{name}, #{location}, #{address}, #{phone}, #{openTime}, #{douyinUrl}, #{tags}, #{cover}, #{licenseImage}, #{gallery}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Club club);

    @Update("update clubs set name = #{name}, location = #{location}, address = #{address}, phone = #{phone}, " +
            "open_time = #{openTime}, douyin_url = #{douyinUrl}, tags = #{tags}, cover = #{cover}, license_image = #{licenseImage}, gallery = #{gallery}, updated_at = now() where id = #{id}")
    int update(Club club);

    @Delete("delete from clubs where id = #{id}")
    int delete(long id);

    @Select("select count(1) from clubs where cover = #{url} or license_image = #{url} or find_in_set(#{url}, gallery)")
    int countImageUsage(@Param("url") String url);
}
