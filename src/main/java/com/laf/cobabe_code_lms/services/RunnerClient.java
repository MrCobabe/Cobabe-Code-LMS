package com.laf.cobabe_code_lms.services;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Service public class RunnerClient {
 private final RestClient client;
 RunnerClient(@Value("${app.runner.url}") String url,@Value("${app.runner.token}") String token){
  client=RestClient.builder().baseUrl(url).defaultHeader("X-Runner-Token",token).build();
 }
 public String run(String code,String stdin){
  try{return client.post().uri("/run").contentType(MediaType.APPLICATION_JSON).body(Map.of("code",code,"stdin",stdin)).retrieve().body(String.class);}
  catch(Exception e){return "Execution service unavailable. Ask your teacher to check the isolated runner.";}
 }
}
