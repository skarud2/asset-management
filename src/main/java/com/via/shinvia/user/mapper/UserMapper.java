package com.via.shinvia.user.mapper;

import com.via.shinvia.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface UserMapper {
    int insertUser(User user);
    boolean existsByLoginEmail(@Param("loginEmail") String loginEmail);
    User findByUserId(@Param("userId") Long userId);
    User findByLoginEmail(@Param("loginEmail") String loginEmail);
    User findIdByNameAndPhone(@Param("userName") String userName, @Param("phoneNumber") String phoneNumber);
    int updatePassword(@Param("loginEmail") String loginEmail, @Param("passwordHash") String passwordHash);
    int updateProfile(
            @Param("userId") Long userId,
            @Param("userName") String userName,
            @Param("phoneNumber") String phoneNumber,
            @Param("birthDate") LocalDate birthDate
    );
}
