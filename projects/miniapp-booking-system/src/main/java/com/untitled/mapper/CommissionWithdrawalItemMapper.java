package com.untitled.mapper;

import com.untitled.model.CommissionWithdrawalItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

public interface CommissionWithdrawalItemMapper {
    @Insert("insert into commission_withdrawal_items (withdrawal_id, commission_record_id, amount, created_at) " +
            "values (#{withdrawalId}, #{commissionRecordId}, #{amount}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommissionWithdrawalItem item);

    @Select("select id, withdrawal_id, commission_record_id, amount, created_at from commission_withdrawal_items where withdrawal_id = #{withdrawalId}")
    List<CommissionWithdrawalItem> findByWithdrawalId(long withdrawalId);

    @Delete("delete from commission_withdrawal_items where withdrawal_id = #{withdrawalId}")
    int deleteByWithdrawalId(long withdrawalId);
}
