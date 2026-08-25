package com.via.shinvia.user.controller;

import com.via.shinvia.user.dto.UserSignupRequestDto;
import com.via.shinvia.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/signup")
@RequiredArgsConstructor
public class UserController {
    private static final String VERIFIED_EMAIL_KEY="VERIFIED_EMAIL";
    private final UserService userService;

    @GetMapping("/email")
    public String emailVerificationFrom(HttpSession session) {
        if (session.getAttribute(VERIFIED_EMAIL_KEY)!=null){
            return "redirect:/signup";
        }
        return "user/email";
    }

    @GetMapping
    public String signupForm(HttpSession session, Model model){
        String verifiedEmail=(String) session.getAttribute( VERIFIED_EMAIL_KEY);

        if (verifiedEmail ==null){
            return "redirect:/email";
        }

        if(!model.containsAttribute("userRequest")) {
            UserSignupRequestDto request = new UserSignupRequestDto();

           request.setLoginEmail(verifiedEmail);
            model.addAttribute("userRequest", request);
        }
        return "user/signup";
    }

    @PostMapping
    public String signup( @Valid @ModelAttribute("userRequest") UserSignupRequestDto request,
                          BindingResult bindingResult,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        String verifiedEmail = (String) session.getAttribute(VERIFIED_EMAIL_KEY);
        if (verifiedEmail==null) {
            return "redirect:/email";
        }

        request.setLoginEmail(verifiedEmail);

        if(bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error->System.out.println(error.getDefaultMessage()));
            return "user/signup";
        }

        try{
            userService.signup(request, verifiedEmail);
            session.removeAttribute(VERIFIED_EMAIL_KEY);
            redirectAttributes.addFlashAttribute("signupMessage", "회원가입이 완료되었습니다.");
            return "redirect:/";
        } catch(IllegalArgumentException e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "user/signup";
        }
    }

    @ModelAttribute("emailVerified")
    public boolean emailVerified(HttpSession session) {
        return session.getAttribute(VERIFIED_EMAIL_KEY) != null;
    }

}
