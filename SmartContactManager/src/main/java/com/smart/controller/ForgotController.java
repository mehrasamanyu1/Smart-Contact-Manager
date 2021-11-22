package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entity.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {

	Random random=new Random(1000);

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	
	//email id form open handler
	@GetMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}

	@PostMapping("/sendOTP")
	public String sendOTP(@RequestParam("email")String email, HttpSession session) {

		int otp=random.nextInt(999999);

		System.out.println("OTP:"+otp);

		String subject="OTP FROM SCM";
		String message=""
				+ "<div style='border:1px solid #e2e2e2; padding: 20px'>"
				+ "<h1>"
				+ "OTP is"
				+ "<b> "+otp
				+ "</b>"
				+ "</h1>"
				+ "</div>";

		String to=email;

		boolean flag=this.emailService.sendEmail(subject, message, to);

		if(flag) {
			session.setAttribute("otp", otp);
			session.setAttribute("email", email);
			return "verifyOTP";
		}else {
			session.setAttribute("message", "Check your email id !!");
			return "forgot_email_form";
		}		
	}

	@PostMapping("/verifyOTP")
	public String verifyOTP(@RequestParam("otp")int otp, HttpSession session) {

		int myOTP=(int) session.getAttribute("otp");
		String email= (String) session.getAttribute("email");
		
		if(myOTP==otp) {
			
			//password change form
			
			User user=this.userRepository.getUserByUserName(email);
			
			if(user==null) {
				//send error message
				session.setAttribute("message", "User does not exist with this email !!");
				return "forgot_email_form";
				
			}else {
				//send change password form
				
				return "password_change_form";	
			}			
		}else {
			session.setAttribute("message", "You have entered a wrong OTP");
			return "verifyOTP";
		}
	}
	
	//change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword")String newPassword,HttpSession session) {
		String email= (String) session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
		this.userRepository.save(user);
		
		session.setAttribute("message", "");
		return "redirect:/signin?change=Password Changed Successfully..";
	}
}
