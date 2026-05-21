package com.untitled.mapper;

import com.untitled.model.CommissionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CommissionRecordMapper {
    @Select("select count(1) from commission_records where order_id = #{orderId}")
    int countByOrderId(long orderId);

    @Select("select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where order_id = #{orderId} order by id asc")
    List<CommissionRecord> findByOrderId(long orderId);

    @Select("<script>" +
            "select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where order_id in " +
            "<foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>#{orderId}</foreach> " +
            "order by order_id asc, id asc" +
            "</script>")
    List<CommissionRecord> findByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Select("select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where order_id = #{orderId} order by id asc for update")
    List<CommissionRecord> findByOrderIdForUpdate(long orderId);

    @Select("<script>" +
            "select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by id desc" +
            "</script>")
    List<CommissionRecord> findByUser(@Param("userId") long userId, @Param("status") String status);

    @Select("<script>" +
            "select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where 1=1 " +
            "<if test='userId != null'>and user_id = #{userId} </if>" +
            "<if test='orderNo != null and orderNo != \"\"'>and order_no = #{orderNo} </if>" +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by id desc" +
            "</script>")
    List<CommissionRecord> findAllForAdmin(@Param("userId") Long userId,
                                           @Param("orderNo") String orderNo,
                                           @Param("status") String status);

    @Select("select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where user_id = #{userId} and available_amount > 0 and status = 'withdrawable' order by available_at asc, id asc for update")
    List<CommissionRecord> findWithdrawableForUpdate(long userId);

    @Select("<script>" +
            "select id, user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time as scan_user_at_order_time, price_amount, merchant_settlement_amount, " +
            "platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at " +
            "from commission_records where id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update" +
            "</script>")
    List<CommissionRecord> findByIdsForUpdate(@Param("ids") List<Long> ids);

    @Insert("insert into commission_records (user_id, order_id, order_no, activity_id, activity_title, is_scan_user_at_order_time, price_amount, " +
            "merchant_settlement_amount, platform_operation_fee, commission_amount, available_amount, frozen_amount, withdrawn_amount, status, occur_time, available_at, reversed_at, reversal_reason, created_at, updated_at) " +
            "values (#{userId}, #{orderId}, #{orderNo}, #{activityId}, #{activityTitle}, #{scanUserAtOrderTime}, #{priceAmount}, #{merchantSettlementAmount}, " +
            "#{platformOperationFee}, #{commissionAmount}, #{availableAmount}, #{frozenAmount}, #{withdrawnAmount}, #{status}, #{occurTime}, #{availableAt}, #{reversedAt}, #{reversalReason}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommissionRecord record);

    @Update("update commission_records set user_id = #{userId}, order_id = #{orderId}, order_no = #{orderNo}, activity_id = #{activityId}, " +
            "activity_title = #{activityTitle}, is_scan_user_at_order_time = #{scanUserAtOrderTime}, price_amount = #{priceAmount}, " +
            "merchant_settlement_amount = #{merchantSettlementAmount}, platform_operation_fee = #{platformOperationFee}, commission_amount = #{commissionAmount}, " +
            "available_amount = #{availableAmount}, frozen_amount = #{frozenAmount}, withdrawn_amount = #{withdrawnAmount}, status = #{status}, occur_time = #{occurTime}, " +
            "available_at = #{availableAt}, reversed_at = #{reversedAt}, reversal_reason = #{reversalReason}, updated_at = now() where id = #{id}")
    int update(CommissionRecord record);

    @Select("select ifnull(sum(withdrawn_amount), 0) from commission_records where status != 'reversed'")
    java.math.BigDecimal sumWithdrawnAmount();

    @Select("select ifnull(sum(available_amount + frozen_amount), 0) from commission_records where status in ('withdrawable', 'pending', 'withdrawing')")
    java.math.BigDecimal sumPendingCommission();
}
