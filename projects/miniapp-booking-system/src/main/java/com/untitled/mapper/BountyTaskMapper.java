package com.untitled.mapper;

import com.untitled.model.BountyTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface BountyTaskMapper {
    @Select("<script>" +
            "select id, location, commission_min as commissionMin, commission_max as commissionMax, " +
            "cover_image_url as coverImageUrl, detail_image_url as detailImageUrl, steps_json as stepsJson, " +
            "step1_text as step1Text, step1_image_url as step1ImageUrl, " +
            "step2_text as step2Text, step2_image_url as step2ImageUrl, " +
            "step3_text as step3Text, step3_image_url as step3ImageUrl, " +
            "status, sort, created_at as createdAt, updated_at as updatedAt " +
            "from bounty_tasks where 1=1 " +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by sort asc, id desc" +
            "</script>")
    List<BountyTask> findAll(@Param("status") String status);

    @Select("select id, location, commission_min as commissionMin, commission_max as commissionMax, " +
            "cover_image_url as coverImageUrl, detail_image_url as detailImageUrl, steps_json as stepsJson, " +
            "step1_text as step1Text, step1_image_url as step1ImageUrl, " +
            "step2_text as step2Text, step2_image_url as step2ImageUrl, " +
            "step3_text as step3Text, step3_image_url as step3ImageUrl, " +
            "status, sort, created_at as createdAt, updated_at as updatedAt " +
            "from bounty_tasks where id = #{id}")
    BountyTask findById(long id);

    @Select("<script>" +
            "select id, location, commission_min as commissionMin, commission_max as commissionMax, " +
            "cover_image_url as coverImageUrl, detail_image_url as detailImageUrl, steps_json as stepsJson, " +
            "step1_text as step1Text, step1_image_url as step1ImageUrl, " +
            "step2_text as step2Text, step2_image_url as step2ImageUrl, " +
            "step3_text as step3Text, step3_image_url as step3ImageUrl, " +
            "status, sort, created_at as createdAt, updated_at as updatedAt " +
            "from bounty_tasks where id in " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<BountyTask> findByIds(@Param("ids") List<Long> ids);

    @Insert("insert into bounty_tasks (location, commission_min, commission_max, cover_image_url, detail_image_url, steps_json, " +
            "step1_text, step1_image_url, step2_text, step2_image_url, step3_text, step3_image_url, status, sort, created_at, updated_at) " +
            "values (#{location}, #{commissionMin}, #{commissionMax}, #{coverImageUrl}, #{detailImageUrl}, #{stepsJson}, " +
            "#{step1Text}, #{step1ImageUrl}, #{step2Text}, #{step2ImageUrl}, #{step3Text}, #{step3ImageUrl}, #{status}, #{sort}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BountyTask bountyTask);

    @Update("update bounty_tasks set location = #{location}, commission_min = #{commissionMin}, commission_max = #{commissionMax}, " +
            "cover_image_url = #{coverImageUrl}, detail_image_url = #{detailImageUrl}, steps_json = #{stepsJson}, " +
            "step1_text = #{step1Text}, step1_image_url = #{step1ImageUrl}, " +
            "step2_text = #{step2Text}, step2_image_url = #{step2ImageUrl}, " +
            "step3_text = #{step3Text}, step3_image_url = #{step3ImageUrl}, " +
            "status = #{status}, sort = #{sort}, updated_at = now() where id = #{id}")
    int update(BountyTask bountyTask);

    @Delete("delete from bounty_tasks where id = #{id}")
    int delete(long id);
}
