package com.untitled.mapper;

import com.untitled.model.CommissionWithdrawal;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CommissionWithdrawalMapper {
    @Select("select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where id = #{id}")
    CommissionWithdrawal findById(long id);

    @Select("select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where id = #{id} for update")
    CommissionWithdrawal findByIdForUpdate(long id);

    @Select("select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where user_id = #{userId} and idem_key = #{idemKey} limit 1")
    CommissionWithdrawal findByUserIdemKey(@Param("userId") long userId, @Param("idemKey") String idemKey);

    @Select("select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where withdraw_no = #{withdrawNo} limit 1")
    CommissionWithdrawal findByWithdrawNo(String withdrawNo);

    @Select("select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where withdraw_no = #{withdrawNo} limit 1 for update")
    CommissionWithdrawal findByWithdrawNoForUpdate(String withdrawNo);

    @Select("<script>" +
            "select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by id desc" +
            "</script>")
    List<CommissionWithdrawal> findByUser(@Param("userId") long userId, @Param("status") String status);

    @Select("<script>" +
            "select id, withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at " +
            "from commission_withdrawals where 1=1 " +
            "<if test='userId != null'>and user_id = #{userId} </if>" +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by id desc" +
            "</script>")
    List<CommissionWithdrawal> findAllForAdmin(@Param("userId") Long userId, @Param("status") String status);

    @Select("select count(1) from commission_withdrawals where user_id = #{userId} and requested_at >= #{start} and requested_at < #{end}")
    int countByUserAndRequestedAt(@Param("userId") long userId,
                                  @Param("start") java.time.LocalDateTime start,
                                  @Param("end") java.time.LocalDateTime end);

    @Select("select ifnull(sum(amount), 0) from commission_withdrawals where user_id = #{userId} and requested_at >= #{start} and requested_at < #{end}")
    java.math.BigDecimal sumAmountByUserAndRequestedAt(@Param("userId") long userId,
                                                       @Param("start") java.time.LocalDateTime start,
                                                       @Param("end") java.time.LocalDateTime end);

    @Insert("insert into commission_withdrawals (withdraw_no, user_id, amount, status, idem_key, fail_reason, transfer_bill_no, transfer_state, transfer_package_info, operator_name, operator_note, requested_at, processed_at, created_at, updated_at) " +
            "values (#{withdrawNo}, #{userId}, #{amount}, #{status}, #{idemKey}, #{failReason}, #{transferBillNo}, #{transferState}, #{transferPackageInfo}, #{operatorName}, #{operatorNote}, #{requestedAt}, #{processedAt}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommissionWithdrawal withdrawal);

    @Update("update commission_withdrawals set withdraw_no = #{withdrawNo}, user_id = #{userId}, amount = #{amount}, status = #{status}, " +
            "idem_key = #{idemKey}, fail_reason = #{failReason}, transfer_bill_no = #{transferBillNo}, transfer_state = #{transferState}, transfer_package_info = #{transferPackageInfo}, " +
            "operator_name = #{operatorName}, operator_note = #{operatorNote}, " +
            "requested_at = #{requestedAt}, processed_at = #{processedAt}, updated_at = now() where id = #{id}")
    int update(CommissionWithdrawal withdrawal);

    @Delete("delete from commission_withdrawals where id = #{id}")
    int deleteById(long id);
}
