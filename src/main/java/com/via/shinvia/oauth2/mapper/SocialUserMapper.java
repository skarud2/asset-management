package com.via.shinvia.oauth2.mapper;

import com.via.shinvia.oauth2.domain.SocialProvider;
import com.via.shinvia.oauth2.domain.SocialUser;
import com.via.shinvia.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SocialUserMapper {
    SocialUser findByProviderAndProviderUserId(@Param("provider") SocialProvider provider,
                                               @Param("providerUserId") String providerUserId);
    int insertSocialUser(SocialUser socialUser);
    List<SocialUser> findAllByUserId(Long userId);
}
