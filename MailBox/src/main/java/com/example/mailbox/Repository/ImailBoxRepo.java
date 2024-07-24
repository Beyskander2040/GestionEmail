package com.example.mailbox.Repository;

import com.example.mailbox.Entity.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImailBoxRepo extends JpaRepository<Mailbox,Long> {
    List<Mailbox> findByUserId(Integer userId);

}
