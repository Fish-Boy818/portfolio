package com.untitled.mapper;

import com.untitled.model.ActivitySlot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

public interface ActivitySlotMapper {
    @Select("<script>" +
            "select id, activity_id, slot_date, slot_time, capacity, booked, status, created_at, updated_at " +
            "from activity_slots where activity_id = #{activityId} " +
            "<if test='slotDate != null'>and slot_date = #{slotDate} </if>" +
            "order by slot_date, slot_time" +
            "</script>")
    List<ActivitySlot> findByActivityId(@Param("activityId") long activityId,
                                        @Param("slotDate") LocalDate slotDate);

    @Select("<script>" +
            "select id, activity_id, slot_date, slot_time, capacity, booked, status, created_at, updated_at " +
            "from activity_slots " +
            "<if test='activityId != null'>where activity_id = #{activityId} </if>" +
            "order by slot_date desc, slot_time" +
            "</script>")
    List<ActivitySlot> findAll(@Param("activityId") Long activityId);

    @Select("select id, activity_id, slot_date, slot_time, capacity, booked, status, created_at, updated_at from activity_slots where id = #{id}")
    ActivitySlot findById(long id);

    @Insert("insert into activity_slots (activity_id, slot_date, slot_time, capacity, booked, status, created_at, updated_at) " +
            "values (#{activityId}, #{slotDate}, #{slotTime}, #{capacity}, #{booked}, #{status}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivitySlot slot);

    @Update("update activity_slots set activity_id = #{activityId}, slot_date = #{slotDate}, slot_time = #{slotTime}, " +
            "capacity = #{capacity}, booked = #{booked}, status = #{status}, updated_at = now() where id = #{id}")
    int update(ActivitySlot slot);

    @Update("update activity_slots set booked = booked + #{delta}, updated_at = now() where id = #{id}")
    int updateBooked(@Param("id") long id, @Param("delta") int delta);

    @Update("update activity_slots set booked = booked + #{delta}, updated_at = now() " +
            "where id = #{id} and booked + #{delta} >= 0 and booked + #{delta} <= capacity")
    int updateBookedSafely(@Param("id") long id, @Param("delta") int delta);

    @Delete("delete from activity_slots where id = #{id}")
    int delete(long id);

    @Delete("<script>" +
            "delete from activity_slots where activity_id in " +
            "<foreach collection='activityIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteByActivityIds(@Param("activityIds") List<Long> activityIds);
}
