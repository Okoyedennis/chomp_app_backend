package com.decagon.chompapp.services.Impl;

import com.decagon.chompapp.dto.EditUserDto;
import com.decagon.chompapp.dto.EmailSenderDto;
import com.decagon.chompapp.dto.ResetPasswordDto;
import com.decagon.chompapp.exception.UserNotFoundException;
import com.decagon.chompapp.models.User;
import com.decagon.chompapp.repository.UserRepository;
import com.decagon.chompapp.security.CustomUserDetailsService;
import com.decagon.chompapp.security.JwtTokenProvider;
import com.decagon.chompapp.services.EmailSenderService;
import com.decagon.chompapp.services.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;


@Service
//@AllArgsConstructor
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final EmailSenderService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<String> editUserDetails(EditUserDto editUserDto) {
        String person = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsernameOrEmail(person, person).get();
        if (editUserDto.getFirstName() != null)
            user.setFirstName(editUserDto.getFirstName());
        if (editUserDto.getLastName() != null)
            user.setLastName(editUserDto.getLastName());
        if (editUserDto.getGender() != null)
            user.setGender(editUserDto.getGender());

        userRepository.save(user);

        String message = "User Details edit successful";

        return new ResponseEntity<>(message, HttpStatus.OK);
    }


    @Override
    public String generateResetToken(String email) throws MessagingException {
        userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User does not exits in the database"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        String token = jwtTokenProvider.generateSignUpConfirmationToken(email);
        EmailSenderDto emailDto = new EmailSenderDto();
        emailDto.setTo(email);
        emailDto.setSubject("Reset Your Password");
        emailDto.setContent( "Please Use This Link to Reset Your Password\n" +
                "http://localhost:8081/api/v1/auth/users/enter-password?token=" + token);
        emailService.send(emailDto);
        return "Check Your Email to Reset Your Password";
    }


    @Override
    public String resetPassword(ResetPasswordDto resetPasswordDto, String token) {
        if (resetPasswordDto.getNewPassword().equals(resetPasswordDto.getConfirmNewPassword())) {
        String email = jwtTokenProvider.getUsernameFromJwt(token);

        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User does not exits in the database"));

        user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        userRepository.save(user);
        return "Password Reset Successfully";
        }
        throw new RuntimeException("Passwords don't match.");
    }
}
