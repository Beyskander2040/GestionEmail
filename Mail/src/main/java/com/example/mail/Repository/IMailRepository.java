package com.example.mail.Repository;

import com.example.mail.Entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface IMailRepository extends PagingAndSortingRepository<Mail,Long> {
    List<Mail> findByUserEmail(String userEmail);

    List<Mail>findByMailboxId(Long mailboxId);

    Optional<Mail> findByUid(String uid);
    Optional<Mail> findByUidAndMailboxId(String uid, Long mailboxId);


    @Query("SELECT COALESCE(MAX(m.receivedDate), NULL) FROM Mail m WHERE m.mailboxId = :mailboxId")
    Date findMostRecentEmailDateByMailboxId(@Param("mailboxId") Long mailboxId);


}
