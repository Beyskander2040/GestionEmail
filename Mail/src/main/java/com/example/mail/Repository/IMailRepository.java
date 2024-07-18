package com.example.mail.Repository;

import com.example.mail.Entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface IMailRepository extends PagingAndSortingRepository<Mail,Long> {

}
