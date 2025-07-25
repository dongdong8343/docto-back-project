package com.ssginc8.docto.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.service.dto.AddDoctorList;
import com.ssginc8.docto.user.service.dto.AddUser;
import com.ssginc8.docto.user.service.dto.AdminUserList;
import com.ssginc8.docto.user.service.dto.CheckPassword;
import com.ssginc8.docto.user.service.dto.EmailVerification;
import com.ssginc8.docto.user.service.dto.FindEmail;
import com.ssginc8.docto.user.service.dto.Login;
import com.ssginc8.docto.user.service.dto.ResetPassword;
import com.ssginc8.docto.user.service.dto.SendVerifyCode;
import com.ssginc8.docto.user.service.dto.SocialSignup;
import com.ssginc8.docto.user.service.dto.UpdateUser;
import com.ssginc8.docto.user.service.dto.UserInfo;

public interface UserService {

	UserInfo.Response getMyInfo();

	void checkEmail(String email);

	FindEmail.Response findEmail(FindEmail.Request request);

	Page<AdminUserList.Response> getUsers(Role role, Pageable pageable);

	AddUser.Response createUser(AddUser.Request request);

	SocialSignup.Response updateSocialInfo(SocialSignup.Request request);

	AddDoctorList.Response registerDoctor(AddDoctorList.Request request);

	Login.Response login(Login.Request request);

	void sendVerificationCode(SendVerifyCode.Request request);

	void confirmVerificationCode(EmailVerification.Request request);

	void resetPassword(ResetPassword.Request request);

	void checkPassword(CheckPassword.Request request);

	void updateInfo(UpdateUser.Request request);

	void deleteAccount();
}
