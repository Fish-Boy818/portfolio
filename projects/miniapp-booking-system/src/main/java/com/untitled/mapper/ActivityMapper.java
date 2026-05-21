package com.untitled.mapper;

import com.untitled.model.Activity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface ActivityMapper {
    @Select("<script>" +
            "select id, club_id, title, subtitle, category, base_price, original_price, merchant_settlement_price, platform_operation_fee, commission_rate, commission_amount, buyer_commission_amount, inviter_commission_amount, " +
            "audience, description, bundle, expire_date, status, cover, gallery, detail_images, created_at, updated_at " +
            "from activities where 1=1 " +
            "<if test='clubId != null'>and club_id = #{clubId} </if>" +
            "<if test='category != null and category != \"\"'>and category = #{category} </if>" +
            "<if test='keyword != null and keyword != \"\"'>and (title like concat('%', #{keyword}, '%') or subtitle like concat('%', #{keyword}, '%')) </if>" +
            "order by id desc" +
            "</script>")
    List<Activity> findAll(@Param("clubId") Long clubId,
                           @Param("category") String category,
                           @Param("keyword") String keyword);

    @Select("select id, club_id, title, subtitle, category, base_price, original_price, merchant_settlement_price, platform_operation_fee, commission_rate, commission_amount, buyer_commission_amount, inviter_commission_amount, " +
            "audience, description, bundle, expire_date, status, cover, gallery, detail_images, created_at, updated_at from activities where id = #{id}")
    Activity findById(long id);

    @Select("<script>" +
            "select id, cover from activities where id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Activity> findByIds(@Param("ids") List<Long> ids);

    @Select("select cover, gallery, detail_images as detailImages from activities")
    List<Map<String, Object>> findAllImageRefs();

    @Insert("insert into activities (club_id, title, subtitle, category, base_price, original_price, merchant_settlement_price, platform_operation_fee, commission_rate, commission_amount, buyer_commission_amount, inviter_commission_amount, " +
            "audience, description, bundle, expire_date, status, cover, gallery, detail_images, created_at, updated_at) " +
            "values (#{clubId}, #{title}, #{subtitle}, #{category}, #{basePrice}, #{originalPrice}, #{merchantSettlementPrice}, #{platformOperationFee}, #{commissionRate}, #{commissionAmount}, #{buyerCommissionAmount}, #{inviterCommissionAmount}, " +
            "#{audience}, #{description}, #{bundle}, #{expireDate}, #{status}, #{cover}, #{gallery}, #{detailImages}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Activity activity);

    @Update("update activities set club_id = #{clubId}, title = #{title}, subtitle = #{subtitle}, category = #{category}, " +
            "base_price = #{basePrice}, original_price = #{originalPrice}, merchant_settlement_price = #{merchantSettlementPrice}, " +
            "platform_operation_fee = #{platformOperationFee}, commission_rate = #{commissionRate}, commission_amount = #{commissionAmount}, buyer_commission_amount = #{buyerCommissionAmount}, inviter_commission_amount = #{inviterCommissionAmount}, audience = #{audience}, description = #{description}, bundle = #{bundle}, expire_date = #{expireDate}, " +
            "status = #{status}, cover = #{cover}, gallery = #{gallery}, detail_images = #{detailImages}, updated_at = now() " +
            "where id = #{id}")
    int update(Activity activity);

    @Delete("delete from activities where id = #{id}")
    int delete(long id);

    @Delete("delete from activities where club_id = #{clubId}")
    int deleteByClubId(long clubId);

    @Select("select id from activities where club_id = #{clubId}")
    List<Long> findIdsByClubId(long clubId);

    @Select("select count(1) from activities where id <> #{excludeId} " +
            "and (cover = #{url} or find_in_set(#{url}, gallery) or find_in_set(#{url}, detail_images))")
    int countImageUsage(@Param("url") String url, @Param("excludeId") long excludeId);
}
