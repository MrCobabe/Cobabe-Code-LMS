package com.laf.cobabe_code_lms;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@Configuration public class SecurityConfig {
 @Value("${app.allowed-domain:}") String domain;
 @Bean SecurityFilterChain chain(HttpSecurity http){
  try {http.authorizeHttpRequests(a->a.requestMatchers("/","/css/**","/js/**","/error").permitAll().anyRequest().authenticated())
  .oauth2Login(o->o.defaultSuccessUrl("/dashboard",true)).logout(l->l.logoutSuccessUrl("/"));return http.build();}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 @Bean OAuth2UserService<OAuth2UserRequest,OAuth2User> oauthUsers(){
  DefaultOAuth2UserService delegate=new DefaultOAuth2UserService();
  return request->{OAuth2User user=delegate.loadUser(request);
   String email=user.getAttribute("email"); Boolean verified=user.getAttribute("email_verified");
   if(email==null || !Boolean.TRUE.equals(verified)) throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"));
   if(!domain.isBlank()){
    String hosted=user.getAttribute("hd");
    if(hosted==null || !hosted.equalsIgnoreCase(domain.trim())) throw new OAuth2AuthenticationException(new OAuth2Error("domain_not_allowed"));
   }
   return user;
  };
 }
}
