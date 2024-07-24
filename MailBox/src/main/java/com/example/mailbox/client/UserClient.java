package com.example.mailbox.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "User")

public interface UserClient {

//    @GetMapping("api/v1/auth/{id}")
//    User getUserById(@PathVariable Integer id);
//    @GetMapping("api/v1/auth/by-username")
//    User getUserByUsername(@RequestParam String username);
}
