package com.bitc502.grapemarket.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bitc502.grapemarket.common.AuthProvider;
import com.bitc502.grapemarket.common.Role;
import com.bitc502.grapemarket.model.User;
import com.bitc502.grapemarket.repository.UserRepository;
import com.bitc502.grapemarket.security.UserPrincipal;
import com.bitc502.grapemarket.service.UserService;
import com.bitc502.grapemarket.util.Script;
import com.bitc502.grapemarket.util.VisitorCounter;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userServ;

	@GetMapping("/login")
	public String loginForm(HttpServletRequest request) {
		if (request.getHeader("DeviceType") != null && request.getHeader("DeviceType").equals("android")) {
			System.out.println("안드로이드 접속");
			VisitorCounter.currentVisitorCount--;
			return "redirect:/android/loginFailure";
		} else {
			return "/user/login";
		}

	}

	@GetMapping("/join")
	public String joinForm() {
		return "/user/join";
	}

	@PostMapping("/usernameCheck")
	public @ResponseBody String usernameCheck(@RequestBody String username) {
		if (userServ.usernameCheck(username) == -1) {
			return "ok";
		}
		return "no";
	}

	@PostMapping("/joinProc")
	public String join(User user, @RequestParam("profile") MultipartFile userProfile) {
		int result = userServ.join(user, userProfile);
		if (result == -1) {
			return "/user/join";
		}
		return "/user/login";
	}

	@GetMapping("/userProfile")
	public String userProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
		User user = userServ.getUserById(userPrincipal.getUser().getId());
		model.addAttribute("user", user);
		return "/user/userProfile";
	}

	@PostMapping("/update")
	public @ResponseBody String update(@AuthenticationPrincipal UserPrincipal userPrincipal, User user,
			@RequestParam("currentUserProfile") String currentUserProfile,
			@RequestParam("profile") MultipartFile userProfile) {
		int result = userServ.update(userPrincipal, user, currentUserProfile, userProfile);
		if (result == -1) {
			return Script.hrefAndAlert("/user/userProfile", "오류 발생");
		}
		return Script.href("/user/userProfile");

	}

	@PostMapping("/addupdate")
	public @ResponseBody String addUpdate(User user, @AuthenticationPrincipal UserPrincipal userPrincipal) {

		int result = userServ.addUpdate(user);
		if (result == -1) {
			return Script.hrefAndAlert("/user/userProfile", "오류 발생");
		}
		userPrincipal.getUser().setAddressAuth(0);
		return Script.href("/user/userProfile");
	}

	@PostMapping("/authupdate")
	public @ResponseBody String authUpdate(User user, @AuthenticationPrincipal UserPrincipal userPrincipal) {

		int result = userServ.addUpdate(user);
		if (result == -1) {
			return Script.hrefAndAlert("/user/userProfile", "오류 발생");
		}
		userPrincipal.getUser().setAddressAuth(1);
		return Script.href("/user/userProfile");
		
		
	}

	@GetMapping("/delete/{id}")
	public @ResponseBody String delete(@PathVariable int id) {
		int result = userServ.delete(id);
		if (result == -1) {
			return Script.back("Fail Delete");
		}
		return Script.href("/test/userAll");
	}

}
