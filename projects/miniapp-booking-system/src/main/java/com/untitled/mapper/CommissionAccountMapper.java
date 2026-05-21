package com.untitled.mapper;

import com.untitled.model.CommissionAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface CommissionAccountMapper {
    @Select("select user_id, withdrawable_balance, pending_balance, withdrawing_balance, withdrawn_total, reversed_total, " +
            "created_at, updated_at from commission_accounts where user_id = #{userId}")
    CommissionAccount findByUserId(long userId);

    @Select("select #{userId} as user_id, " +
            "ifnull(sum(case when status = 'withdrawable' then available_amount else 0 end), 0) as withdrawable_balance, " +
            "ifnull(sum(case when status = 'pending' then commission_amount else 0 end), 0) as pending_balance, " +
            "ifnull(sum(frozen_amount), 0) as withdrawing_balance, " +
            "ifnull(sum(withdrawn_amount), 0) as withdrawn_total, " +
            "ifnull(sum(case when status = 'reversed' then commission_amount else 0 end), 0) as reversed_total " +
            "from commission_records where user_id = #{userId}")
    CommissionAccount calculateByUserId(long userId);

    @Insert("insert into commission_accounts (user_id, withdrawable_balance, pending_balance, withdrawing_balance, withdrawn_total, reversed_total, created_at, updated_at) " +
            "values (#{userId}, #{withdrawableBalance}, #{pendingBalance}, #{withdrawingBalance}, #{withdrawnTotal}, #{reversedTotal}, now(), now()) " +
            "on duplicate key update withdrawable_balance = values(withdrawable_balance), pending_balance = values(pending_balance), " +
            "withdrawing_balance = values(withdrawing_balance), withdrawn_total = values(withdrawn_total), reversed_total = values(reversed_total), updated_at = now()")
    int upsert(CommissionAccount account);
}
