package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@RestController
public class AppController {

    private static final String SPRING_APPLICATION_NAME = "docker-app-b";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping
    public String isRunning() throws UnknownHostException {
        return "Hi, from docker-microservice is running from =" + InetAddress.getLocalHost() + " [container_id/container_ip]";
    }

    @GetMapping("/services")
    public List<String> showServices() {
        return discoveryClient.getServices();
    }

    @GetMapping("/call-docker-app-b")
    public String callDockerAppB(){
        return restTemplate.getForObject("http://"+ SPRING_APPLICATION_NAME, String.class);
    }

}
