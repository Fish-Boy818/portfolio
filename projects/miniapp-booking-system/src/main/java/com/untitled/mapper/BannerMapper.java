package com.untitled.mapper;

import com.untitled.model.Banner;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface BannerMapper {
    @Select("select id, title, subtitle, image_url, status, sort, created_at, updated_at " +
            "from banners order by sort asc, id asc")
    List<Banner> findAll();

    @Select("select id, title, subtitle, image_url, status, sort, created_at, updated_at " +
            "from banners where status = 'active' order by sort asc, id asc")
    List<Banner> findActive();

    @Select("select id, title, subtitle, image_url, status, sort, created_at, updated_at from banners where id = #{id}")
    Banner findById(long id);

    @Select("select image_url from banners")
    List<String> findAllImageUrls();

    @Insert("insert into banners (title, subtitle, image_url, status, sort, created_at, updated_at) " +
            "values (#{title}, #{subtitle}, #{imageUrl}, #{status}, #{sort}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Banner banner);

    @Update("update banners set title = #{title}, subtitle = #{subtitle}, image_url = #{imageUrl}, " +
            "status = #{status}, sort = #{sort}, updated_at = now() where id = #{id}")
    int update(Banner banner);

    @Delete("delete from banners where id = #{id}")
    int delete(long id);

    @Select("select count(1) from banners where image_url = #{url}")
    int countImageUsage(@Param("url") String url);
}
