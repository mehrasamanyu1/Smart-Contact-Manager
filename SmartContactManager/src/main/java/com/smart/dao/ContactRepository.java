package com.smart.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entity.Contact;
import com.smart.entity.User;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

	//pagination...
	@Query("from Contact as c where c.user.id=:userId")
	//it will contain:
	//currentPage - page object
	//Contact Per Page - 5
	public Page<Contact> findContactsByUser(@Param("userId")int userId, Pageable pageable);
	
	//search
	public List<Contact> findByNameContainingAndUser(String name,User user);
}
