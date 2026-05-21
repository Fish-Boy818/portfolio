package com.untitled.mapper;

import com.untitled.model.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserMapper {
    @Select("select id, open_id, nickname, avatar_url, phone, depth, is_scan_user as scan_user, scan_activated_at, inviter_id, invited_at, created_at, updated_at from users order by id")
    List<User> findAll();

    @Select("select id, open_id, nickname, avatar_url, phone, depth, is_scan_user as scan_user, scan_activated_at, inviter_id, invited_at, created_at, updated_at from users where id = #{id}")
    User findById(long id);

    @Select("<script>" +
            "select id, open_id, nickname, avatar_url, phone, depth, is_scan_user as scan_user, scan_activated_at, inviter_id, invited_at, created_at, updated_at " +
            "from users where id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<User> findByIds(@Param("ids") List<Long> ids);

    @Select("select id, open_id, nickname, avatar_url, phone, depth, is_scan_user as scan_user, scan_activated_at, inviter_id, invited_at, created_at, updated_at from users where open_id = #{openId}")
    User findByOpenId(String openId);

    @Insert("insert into users (open_id, nickname, avatar_url, phone, depth, is_scan_user, scan_activated_at, inviter_id, invited_at, created_at, updated_at) " +
            "values (#{openId}, #{nickname}, #{avatarUrl}, #{phone}, #{depth}, ifnull(#{scanUser}, 0), #{scanActivatedAt}, #{inviterId}, #{invitedAt}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("update users set nickname = #{nickname}, avatar_url = #{avatarUrl}, phone = #{phone}, depth = #{depth}, is_scan_user = #{scanUser}, " +
            "scan_activated_at = #{scanActivatedAt}, inviter_id = #{inviterId}, invited_at = #{invitedAt}, updated_at = now() where id = #{id}")
    int update(User user);

    @Update("update users set is_scan_user = 1, scan_activated_at = ifnull(scan_activated_at, now()), updated_at = now() " +
            "where id = #{id} and (is_scan_user is null or is_scan_user <> 1)")
    int activateScanUser(long id);

    @Update("update users set inviter_id = #{inviterId}, invited_at = ifnull(invited_at, now()), updated_at = now() " +
            "where id = #{userId} and id <> #{inviterId} and (inviter_id is null or inviter_id = 0)")
    int bindInviter(@Param("userId") long userId, @Param("inviterId") long inviterId);

    @Select("select count(1) from users where inviter_id = #{inviterId}")
    int countByInviterId(long inviterId);

    @Select("select count(1) from users")
    int countAll();

    @Select("select id, open_id, nickname, avatar_url, phone, depth, is_scan_user as scan_user, scan_activated_at, inviter_id, invited_at, created_at, updated_at " +
            "from users where inviter_id = #{inviterId} order by invited_at desc, id desc")
    List<User> findByInviterId(long inviterId);

    @Delete("delete from users where id = #{id}")
    int delete(long id);
}
