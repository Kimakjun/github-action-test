package com.example.githubactiontest.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class HelloContorller {

    @GetMapping("/hello")
    fun hello() = "Hello world!!!!!!"

}