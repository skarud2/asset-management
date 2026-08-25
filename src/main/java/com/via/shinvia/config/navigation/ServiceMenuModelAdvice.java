package com.via.shinvia.config.navigation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class ServiceMenuModelAdvice {

    private final ServiceMenuProvider serviceMenuProvider;

    @ModelAttribute("loanServiceMenus")
    public List<ServiceMenuCategory> loanServiceMenus() {
        return serviceMenuProvider.loanMenus();
    }

    @ModelAttribute("assetServiceMenus")
    public List<ServiceMenuCategory> assetServiceMenus() {
        return serviceMenuProvider.assetMenus();
    }

    @ModelAttribute("currentRequestPath")
    public String currentRequestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
