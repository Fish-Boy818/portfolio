package com.untitled.mapper;

import com.untitled.model.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderMapper {
    @Select("<script>" +
            "select id, order_no, user_id, activity_id, club_id, slot_id, phone, title, club_name, club_location, slot_date, slot_time, status, " +
            "price_level, unit_price, original_price, quantity, pay_amount, is_scan_user_at_order_time as scan_user_at_order_time, merchant_settlement_amount, " +
            "platform_operation_fee, user_commission_amount, verify_code, paid_at, verified_at, refund_at, cancelled_at, created_at, updated_at " +
            "from orders where 1=1 " +
            "<if test='userId != null'>and user_id = #{userId} </if>" +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by created_at desc" +
            "</script>")
    List<Order> findAll(@Param("userId") Long userId, @Param("status") String status);

    @Select("select id, order_no, user_id, activity_id, club_id, slot_id, phone, title, club_name, club_location, slot_date, slot_time, status, " +
            "price_level, unit_price, original_price, quantity, pay_amount, is_scan_user_at_order_time as scan_user_at_order_time, merchant_settlement_amount, " +
            "platform_operation_fee, user_commission_amount, verify_code, paid_at, verified_at, refund_at, cancelled_at, created_at, updated_at " +
            "from orders where id = #{id}")
    Order findById(long id);

    @Select("select id, order_no, user_id, activity_id, club_id, slot_id, phone, title, club_name, club_location, slot_date, slot_time, status, " +
            "price_level, unit_price, original_price, quantity, pay_amount, is_scan_user_at_order_time as scan_user_at_order_time, merchant_settlement_amount, " +
            "platform_operation_fee, user_commission_amount, verify_code, paid_at, verified_at, refund_at, cancelled_at, created_at, updated_at " +
            "from orders where order_no = #{orderNo} limit 1")
    Order findByOrderNo(String orderNo);

    @Select("select id, order_no, user_id, activity_id, club_id, slot_id, phone, title, club_name, club_location, slot_date, slot_time, status, " +
            "price_level, unit_price, original_price, quantity, pay_amount, is_scan_user_at_order_time as scan_user_at_order_time, merchant_settlement_amount, " +
            "platform_operation_fee, user_commission_amount, verify_code, paid_at, verified_at, refund_at, cancelled_at, created_at, updated_at " +
            "from orders where user_id = #{userId} and activity_id = #{activityId} and status = 'unpaid' " +
            "and quantity = #{quantity} and pay_amount = #{payAmount} and phone = #{phone} " +
            "and ((#{slotId} is null and slot_id is null) or slot_id = #{slotId}) and created_at >= #{createdAfter} " +
            "order by id desc limit 1")
    Order findRecentUnpaidDuplicate(@Param("userId") Long userId,
                                    @Param("activityId") Long activityId,
                                    @Param("slotId") Long slotId,
                                    @Param("phone") String phone,
                                    @Param("quantity") Integer quantity,
                                    @Param("payAmount") BigDecimal payAmount,
                                    @Param("createdAfter") LocalDateTime createdAfter);

    @Select("select sum(pay_amount) from orders where user_id = #{userId} and status = 'used'")
    BigDecimal sumUsedPayAmount(long userId);

    @Insert("insert into orders (order_no, user_id, activity_id, club_id, slot_id, phone, title, club_name, club_location, slot_date, slot_time, status, " +
            "price_level, unit_price, original_price, quantity, pay_amount, is_scan_user_at_order_time, merchant_settlement_amount, platform_operation_fee, " +
            "user_commission_amount, verify_code, paid_at, verified_at, refund_at, cancelled_at, created_at, updated_at) " +
            "values (#{orderNo}, #{userId}, #{activityId}, #{clubId}, #{slotId}, #{phone}, #{title}, #{clubName}, #{clubLocation}, #{slotDate}, #{slotTime}, #{status}, " +
            "#{priceLevel}, #{unitPrice}, #{originalPrice}, #{quantity}, #{payAmount}, #{scanUserAtOrderTime}, #{merchantSettlementAmount}, #{platformOperationFee}, " +
            "#{userCommissionAmount}, #{verifyCode}, #{paidAt}, #{verifiedAt}, #{refundAt}, #{cancelledAt}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("update orders set status = #{status}, price_level = #{priceLevel}, unit_price = #{unitPrice}, original_price = #{originalPrice}, " +
            "quantity = #{quantity}, pay_amount = #{payAmount}, is_scan_user_at_order_time = #{scanUserAtOrderTime}, " +
            "merchant_settlement_amount = #{merchantSettlementAmount}, platform_operation_fee = #{platformOperationFee}, " +
            "user_commission_amount = #{userCommissionAmount}, verify_code = #{verifyCode}, paid_at = #{paidAt}, verified_at = #{verifiedAt}, " +
            "refund_at = #{refundAt}, cancelled_at = #{cancelledAt}, updated_at = now() where id = #{id}")
    int update(Order order);

    @Select("select id from orders where status = 'unpaid' and created_at <= #{expireBefore}")
    List<Long> findExpiredUnpaidIds(@Param("expireBefore") LocalDateTime expireBefore);

    @Delete("delete from orders where id = #{id}")
    int delete(long id);

    @Select("select ifnull(sum(pay_amount), 0) from orders where status in ('paid', 'used')")
    BigDecimal sumTotalSales();

    @Select("select ifnull(sum(pay_amount), 0) from orders where status = 'used'")
    BigDecimal sumVerifiedAmount();

    @Select("select ifnull(sum(pay_amount), 0) from orders where status = 'paid'")
    BigDecimal sumUnverifiedAmount();
}
