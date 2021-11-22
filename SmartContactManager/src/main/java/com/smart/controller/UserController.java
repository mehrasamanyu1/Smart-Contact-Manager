package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import com.razorpay.*;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entity.Contact;
import com.smart.entity.MyOrder;
import com.smart.entity.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private MyOrderRepository myOrderRepository;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommanData(Model model,Principal principal) {
		String username=principal.getName();
		System.out.println("USERNAME: "+username);

		//get the user using username(Email)
		User user=userRepository.getUserByUserName(username);

		System.out.println("USER "+user);

		model.addAttribute("user",user);

	}

	//dashboard home
	@GetMapping("/index")
	public String dashboard(Model model,Principal principal) {
		model.addAttribute("title","User_dashboard - Smart Contact Manager");
		return "normal/user_dashboard";
	}

	//open add contact form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model,Principal principal) {
		model.addAttribute("title","Add Contact - Smart Contact Manager");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}

	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage")MultipartFile image,
			Principal principal,HttpSession session) {
		try {
			String name=principal.getName();
			User user=this.userRepository.getUserByUserName(name);

			//processing and uploading file...
			if(image.isEmpty()) {
				//if the file is empty then try our message
				System.out.println("File is empty.");	
				contact.setImage("contact.png");
			}else {
				//put the file to folder and update the name to contact
				contact.setImage(image.getOriginalFilename());

				File file=new ClassPathResource("static/img").getFile();

				Path path=Paths.get(file.getAbsolutePath()+File.separator+image.getOriginalFilename());

				Files.copy(image.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image uploaded successfully.");
			}

			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);

			System.out.println("CONTACT: "+contact);

			System.out.println("ADDED TO THE DATABASE");

			//success message
			session.setAttribute("message", new Message("Contact added succesfully !!", "success"));

		}catch(Exception e) {
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("Something went wrong!! Try again..", "danger"));
		}
		return "normal/add_contact_form";
	}

	//show contacts handler
	//per page 5[n] contacts
	//current page = 0 [page]

	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page")Integer page, Model model,Principal principal) {
		model.addAttribute("title","Show Contacts - Smart Contact Manager");
		String username=principal.getName();
		User user=this.userRepository.getUserByUserName(username);

		Pageable pageable=PageRequest.of(page, 3);

		Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(),pageable);

		model.addAttribute("contacts",contacts);
		model.addAttribute("currentPage",page);		
		model.addAttribute("totalPages",contacts.getTotalPages());

		return "normal/show_contacts";
	}

	//showing particular contact details.
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId")Integer cId,Model model,Principal principal) {	

		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();

		String username = principal.getName();

		User user=this.userRepository.getUserByUserName(username);

		if(user.getId() == contact.getUser().getId())
			model.addAttribute("contact",contact);

		System.out.println("User: "+user.getId()+" Contact: "+contact.getUser().getId());

		model.addAttribute("title",contact.getName()+" Contact Details - Smart Contact Manager");

		return "normal/contact_detail";
	}

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId")Integer cId,Model model,Principal principal,HttpSession session) throws IOException {
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();

		String username = principal.getName();

		User user=this.userRepository.getUserByUserName(username);

		user.getContacts().remove(contact);

		this.userRepository.save(user);

		File deleteFile=new ClassPathResource("static/img").getFile();
		File file1=new File(deleteFile,contact.getImage());
		file1.delete();

		session.setAttribute("message", new Message("Contact Deleted Successfully.", "success"));

		return "redirect:/user/show-contacts/0	";
	}

	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId")Integer cId,Model model) {
		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("title","Update Contact - Smart Contact Manager");
		model.addAttribute("contact",contact);
		return "normal/update_form";
	}

	@PostMapping("/process-update")
	public String processUpdate(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage")MultipartFile image,
			Principal principal,HttpSession session,
			Model model) {
		try {

			Contact oldContact = this.contactRepository.findById(contact.getcId()).get();

			if(!image.isEmpty()) {
				//put the file to folder and update the name to contact

				File deleteFile=new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldContact.getImage());
				file1.delete();

				contact.setImage(image.getOriginalFilename());

				File file=new ClassPathResource("static/img").getFile();

				Path path=Paths.get(file.getAbsolutePath()+File.separator+image.getOriginalFilename());

				Files.copy(image.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);

			}else {
				contact.setImage(oldContact.getImage());
			}

			String name=principal.getName();
			User user=this.userRepository.getUserByUserName(name);

			contact.setUser(user);
			this.contactRepository.save(contact);

			System.out.println("CONTACT: "+contact);

			System.out.println("ADDED TO THE DATABASE");

			//success message
			session.setAttribute("message", new Message("Contact updated succesfully !!", "success"));

		}catch(Exception e) {
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("Something went wrong!! Try again..", "danger"));
		}
		return "redirect:/user/"+contact.getcId()+"/contact";
	}

	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title","Profile Page - Smart Contact Manager");
		return "normal/profile";
	}

	@PostMapping("/update-user/{id}")
	public String updateUser(@PathVariable("id")Integer id,Model model) {
		User user = this.userRepository.findById(id).get();
		model.addAttribute("title","Update Profile - Smart Contact Manager");
		model.addAttribute("user",user);
		model.addAttribute("updateProfile",true);
		return "normal/settings";
	}

	@PostMapping("/user-process-update")
	public String processUpdate(
			@ModelAttribute User user,
			@RequestParam("profileImage")MultipartFile image,
			Principal principal,HttpSession session,
			Model model) {
		try {

			User oldUser = this.userRepository.findById(user.getId()).get();

			if(!image.isEmpty()) {
				//put the file to folder and update the name to contact

				File deleteFile=new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldUser.getImageURL());
				file1.delete();

				user.setImageURL(image.getOriginalFilename());

				File file=new ClassPathResource("static/img").getFile();

				Path path=Paths.get(file.getAbsolutePath()+File.separator+image.getOriginalFilename());

				Files.copy(image.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);

			}else {
				user.setImageURL(oldUser.getImageURL());
			}

			this.userRepository.save(user);

			//success message
			session.setAttribute("message", new Message("Profile Updated Succesfully !!", "success"));

		}catch(Exception e) {
			e.printStackTrace();
			//error message
			session.setAttribute("message", new Message("Something went wrong!! Try again..", "danger"));
		}
		return "redirect:/user/profile";
	}

	//open settings handler
	@GetMapping("/settings")
	public String openSettings(Model model) {
		model.addAttribute("title","Settings - Smart Contact Manager");
		return "normal/settings";
	}

	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword")String oldPassword,
			@RequestParam("newPassword")String newPassword,
			Model model,Principal principal,HttpSession session) {

		String userName=principal.getName();
		User currentUser=this.userRepository.getUserByUserName(userName);
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())){
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message",new Message("Your Password is Updated Successfully...","success"));
		}else {
			session.setAttribute("message",new Message("Please Enter correct old Password","danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}

	//creating order for payment
	@PostMapping("/create-order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws RazorpayException {
		System.out.println("Done");
		System.out.println(data);
		
		int amount=Integer.parseInt(data.get("amount").toString());
		var client=new RazorpayClient("rzp_test_mWuBoSt0xmHBbJ", "rkGKEeSc31BxP7hV5ttIi5tM");
		
		JSONObject ob=new JSONObject();
		ob.put("amount", amount*100);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_23231");
		
		//creating new order
		Order order=client.Orders.create(ob);
		System.out.println(order);
		
		//if you want you can save this to your database...
		//save the order
		
		MyOrder myOrder= new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);
		
		return order.toString();
	}
	
	@PostMapping("/update-order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String,Object> data){
		MyOrder order=this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		order.setPaymentId(data.get("payment_id").toString());
		order.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(order);
		
		System.out.println(data);
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
}

