package com.ssginc8.docto.auth.service;

import java.util.Map;
import java.util.Objects;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.auth.entity.CustomOAuth2User;
import com.ssginc8.docto.auth.service.dto.OAuthAttributes;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 로그인 성공 후 로그인 정보 가져와 가공하는 클래스
@Log4j2
@RequiredArgsConstructor
@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

	private final UserRepository userRepository;
	private final UserProvider userProvider;

	@Transactional
	@Override
	public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
		// 소셜 로그인 한 사용자 정보 가져오기
		OAuth2User oAuth2User = super.loadUser(request);

		// 클라이언트의 이름 가져오기 (naver, kakao)
		String oauthClientName = request.getClientRegistration().getClientName();

		try {
			log.info("-----------------------소셜 로그인 정보-------------------------");
			log.info(oauthClientName);
			log.info(new ObjectMapper().writeValueAsString(oAuth2User.getAttributes()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		OAuthAttributes oAuthAttributes = null;

		if (oauthClientName.equals("Kakao")) {
			Map<String, Object> kakaoAcount = (Map<String, Object>)oAuth2User.getAttributes().get("kakao_account");

			oAuthAttributes =
				OAuthAttributes.oAuthAttributesByKakao(oAuth2User.getAttributes().get("id").toString(),
					kakaoAcount.get("email").toString(), kakaoAcount.get("name").toString());
		}

		if (oauthClientName.equals("Naver")) {
			Map<String, Object> naverAccount = (Map<String, Object>)oAuth2User.getAttributes().get("response");

			oAuthAttributes =
				OAuthAttributes.oAuthAttributesByNaver(naverAccount.get("id").toString(),
					naverAccount.get("email").toString(), naverAccount.get("name").toString());
		}

		if (Objects.nonNull(oAuthAttributes)) {
			User user = saveOrUpdate(oAuthAttributes);
			userProvider.createUser(user);
		}

		return new CustomOAuth2User(oAuthAttributes.getProviderId());
	}

	private User saveOrUpdate(OAuthAttributes attributes) {
		User user = userProvider.loadUserByEmail(attributes.getEmail())
			.map(entity -> entity.updateOauthInfo(attributes.getProviderId(), attributes.getName(),
				attributes.getEmail(), attributes.getLoginType()))
			.orElse(attributes.toEntity());

		return user;
	}
}
