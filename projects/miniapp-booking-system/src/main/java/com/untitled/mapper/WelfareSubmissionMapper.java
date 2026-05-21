package com.untitled.mapper;

import com.untitled.model.WelfareSubmission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface WelfareSubmissionMapper {
    @Select("select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where id = #{id}")
    WelfareSubmission findById(long id);

    @Select("select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where id = #{id} for update")
    WelfareSubmission findByIdForUpdate(long id);

    @Select("select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where user_id = #{userId} order by id desc")
    List<WelfareSubmission> findByUserId(long userId);

    @Select("<script>" +
            "select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where id in " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<WelfareSubmission> findByIds(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where user_id = #{userId} and bounty_task_id in " +
            "<foreach item='taskId' collection='taskIds' open='(' separator=',' close=')'>#{taskId}</foreach> " +
            "order by bounty_task_id asc, id desc" +
            "</script>")
    List<WelfareSubmission> findLatestByUserIdAndTaskIds(@Param("userId") long userId, @Param("taskIds") List<Long> taskIds);

    @Select("<script>" +
            "select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where user_id = #{userId} and bounty_task_id in " +
            "<foreach item='taskId' collection='taskIds' open='(' separator=',' close=')'>#{taskId}</foreach> " +
            "order by created_at desc, id desc" +
            "</script>")
    List<WelfareSubmission> findAllByUserIdAndTaskIds(@Param("userId") long userId, @Param("taskIds") List<Long> taskIds);

    @Select("<script>" +
            "select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where 1=1 " +
            "<if test='userId != null'>and user_id = #{userId} </if>" +
            "<if test='status != null and status != \"\"'>and status = #{status} </if>" +
            "order by id desc" +
            "</script>")
    List<WelfareSubmission> findAllForAdmin(@Param("userId") Long userId, @Param("status") String status);

    @Select("<script>" +
            "select ws.id as submissionId, " +
            "(select count(1) " +
            "   from welfare_submissions ws2 " +
            "   join users u2 on u2.id = ws2.user_id " +
            "  where coalesce(nullif(u2.phone, ''), concat('UID#', ws2.user_id)) = coalesce(nullif(u.phone, ''), concat('UID#', ws.user_id)) " +
            "    and coalesce(nullif(ws2.bounty_location, ''), '#') = coalesce(nullif(ws.bounty_location, ''), '#') " +
            "    and ws2.id &lt;= ws.id) as submissionAttempt " +
            "from welfare_submissions ws " +
            "join users u on u.id = ws.user_id " +
            "where ws.id in " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> findAttemptCountsByIds(@Param("ids") List<Long> ids);

    @Select("select id, submission_no as submissionNo, user_id as userId, bounty_task_id as bountyTaskId, bounty_location as bountyLocation, platform_name as platformName, review_text as reviewText, " +
            "screenshot_url as screenshotUrl, status, reward_amount as rewardAmount, review_note as reviewNote, reviewed_by as reviewedBy, " +
            "reviewed_at as reviewedAt, reward_record_id as rewardRecordId, rewarded_at as rewardedAt, delete_image_at as deleteImageAt, " +
            "image_deleted as imageDeleted, created_at as createdAt, updated_at as updatedAt " +
            "from welfare_submissions where image_deleted = 0 and delete_image_at is not null and delete_image_at <= #{now} order by delete_image_at asc")
    List<WelfareSubmission> findExpiredImages(@Param("now") LocalDateTime now);

    @Select("select screenshot_url from welfare_submissions where image_deleted = 0 and screenshot_url is not null and screenshot_url != ''")
    List<String> findAllImageUrls();

    @Insert("insert into welfare_submissions (submission_no, user_id, bounty_task_id, bounty_location, platform_name, review_text, screenshot_url, status, reward_amount, review_note, reviewed_by, reviewed_at, reward_record_id, rewarded_at, delete_image_at, image_deleted, created_at, updated_at) " +
            "values (#{submissionNo}, #{userId}, #{bountyTaskId}, #{bountyLocation}, #{platformName}, #{reviewText}, #{screenshotUrl}, #{status}, #{rewardAmount}, #{reviewNote}, #{reviewedBy}, #{reviewedAt}, #{rewardRecordId}, #{rewardedAt}, #{deleteImageAt}, #{imageDeleted}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WelfareSubmission submission);

    @Update("update welfare_submissions set submission_no = #{submissionNo}, user_id = #{userId}, bounty_task_id = #{bountyTaskId}, bounty_location = #{bountyLocation}, platform_name = #{platformName}, review_text = #{reviewText}, " +
            "screenshot_url = #{screenshotUrl}, status = #{status}, reward_amount = #{rewardAmount}, review_note = #{reviewNote}, reviewed_by = #{reviewedBy}, " +
            "reviewed_at = #{reviewedAt}, reward_record_id = #{rewardRecordId}, rewarded_at = #{rewardedAt}, delete_image_at = #{deleteImageAt}, image_deleted = #{imageDeleted}, " +
            "updated_at = now() where id = #{id}")
    int update(WelfareSubmission submission);
}
