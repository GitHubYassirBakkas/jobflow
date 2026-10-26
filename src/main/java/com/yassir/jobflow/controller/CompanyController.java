package com.yassir.jobflow.controller;

import com.yassir.jobflow.dto.CompanyResponse;
import com.yassir.jobflow.dto.CreateCompanyRequest;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yassir.jobflow.entity.User;
import com.yassir.jobflow.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
private final UserService userService;

public CompanyController(
        CompanyService companyService,
        UserService userService) {

    this.companyService = companyService;
    this.userService = userService;
}
    @PostMapping
public ResponseEntity<CompanyResponse> createCompany(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateCompanyRequest request) {

    User user = userService.getUserByEmail(jwt.getSubject())
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    Company company = companyService.createCompany(request, user);

    CompanyResponse response = new CompanyResponse(
            company.getId(),
            company.getName(),
            company.getDescription(),
            company.getWebsite(),
            company.getLocation(),
            company.getCreatedAt()
    );

    return ResponseEntity.ok(response);
}
}