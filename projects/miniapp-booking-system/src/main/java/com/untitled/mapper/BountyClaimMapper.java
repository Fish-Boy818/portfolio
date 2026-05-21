package com.untitled.mapper;

import com.untitled.model.BountyClaim;
import com.untitled.model.BountyClaimStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface BountyClaimMapper {
    @Select("select id, bounty_task_id as bountyTaskId, user_id as userId, status, submit_count as submitCount, latest_submission_id as latestSubmissionId, " +
            "accepted_at as acceptedAt, submitted_at as submittedAt, reviewed_at as reviewedAt, created_at as createdAt, updated_at as updatedAt " +
            "from bounty_claims where bounty_task_id = #{bountyTaskId} and user_id = #{userId} limit 1")
    BountyClaim findByUserIdAndTaskId(@Param("userId") long userId, @Param("bountyTaskId") long bountyTaskId);

    @Select("<script>" +
            "select id, bounty_task_id as bountyTaskId, user_id as userId, status, submit_count as submitCount, latest_submission_id as latestSubmissionId, " +
            "accepted_at as acceptedAt, submitted_at as submittedAt, reviewed_at as reviewedAt, created_at as createdAt, updated_at as updatedAt " +
            "from bounty_claims where user_id = #{userId} and bounty_task_id in " +
            "<foreach item='taskId' collection='taskIds' open='(' separator=',' close=')'>#{taskId}</foreach>" +
            "</script>")
    List<BountyClaim> findByUserIdAndTaskIds(@Param("userId") long userId, @Param("taskIds") List<Long> taskIds);

    @Select("select id, bounty_task_id as bountyTaskId, user_id as userId, status, submit_count as submitCount, latest_submission_id as latestSubmissionId, " +
            "accepted_at as acceptedAt, submitted_at as submittedAt, reviewed_at as reviewedAt, created_at as createdAt, updated_at as updatedAt " +
            "from bounty_claims where user_id = #{userId} order by updated_at desc, id desc")
    List<BountyClaim> findByUserId(long userId);

    @Select("<script>" +
            "select bounty_task_id as bountyTaskId, status, count(1) as total " +
            "from bounty_claims where bounty_task_id in " +
            "<foreach item='taskId' collection='taskIds' open='(' separator=',' close=')'>#{taskId}</foreach> " +
            "group by bounty_task_id, status" +
            "</script>")
    List<BountyClaimStat> countByTaskIds(@Param("taskIds") List<Long> taskIds);

    @Insert("insert into bounty_claims (bounty_task_id, user_id, status, submit_count, latest_submission_id, accepted_at, submitted_at, reviewed_at, created_at, updated_at) " +
            "values (#{bountyTaskId}, #{userId}, #{status}, #{submitCount}, #{latestSubmissionId}, #{acceptedAt}, #{submittedAt}, #{reviewedAt}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BountyClaim claim);

    @Update("update bounty_claims set status = #{status}, submit_count = #{submitCount}, latest_submission_id = #{latestSubmissionId}, accepted_at = #{acceptedAt}, " +
            "submitted_at = #{submittedAt}, reviewed_at = #{reviewedAt}, updated_at = now() where id = #{id}")
    int update(BountyClaim claim);
}
