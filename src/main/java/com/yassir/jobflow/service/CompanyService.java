package com.yassir.jobflow.service;

import com.yassir.jobflow.dto.CreateCompanyRequest;
import com.yassir.jobflow.entity.Company;
import com.yassir.jobflow.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import com.yassir.jobflow.dto.UpdateCompanyRequest;
import com.yassir.jobflow.exception.ResourceNotFoundException;
import com.yassir.jobflow.entity.User;
import java.util.List;


@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company createCompany(
        CreateCompanyRequest request,
        User createdBy) {

    Company company = new Company();

    company.setName(request.getName());
    company.setDescription(request.getDescription());
    company.setWebsite(request.getWebsite());
    company.setLocation(request.getLocation());
    company.setCreatedBy(createdBy);

    return companyRepository.save(company);
}

    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                // الجديد
                .orElseThrow(() ->
                        new ResourceNotFoundException("Company not found"));
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company updateCompany(
        Long companyId,
        UpdateCompanyRequest request) {

    Company company = companyRepository.findById(companyId)
            // الجديد
            .orElseThrow(() ->
                    new ResourceNotFoundException("Company not found")
            );

    company.setName(request.getName());
    company.setDescription(request.getDescription());
    company.setWebsite(request.getWebsite());
    company.setLocation(request.getLocation());

    return companyRepository.save(company);
}

}