package com.example.mail.Repository;

import com.example.mail.Entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface IMailRepository extends PagingAndSortingRepository<Mail,Long> {
    List<Mail> findByUserEmail(String userEmail);
    List<Mail> findByMailboxId(Long mailboxId);



}
