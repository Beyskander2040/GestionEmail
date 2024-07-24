package com.example.mail.Repository;

import com.example.mail.Entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface IMailRepository extends PagingAndSortingRepository<Mail,Long> {
    List<Mail> findByUserEmail(String userEmail);
    List<Mail> findByUserId(Integer userId);



}
