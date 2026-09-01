package com.pnc.masters.salesperson;

import com.pnc.masters.salesperson.api.SalesPersonNotFoundException;
import com.pnc.masters.salesperson.api.SalesPersonRequest;
import com.pnc.masters.salesperson.api.SalesPersonResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SalesPersonService {

    private final SalesPersonRepository salesPersonRepository;

    public SalesPersonService(SalesPersonRepository salesPersonRepository) {
        this.salesPersonRepository = salesPersonRepository;
    }

    @Transactional(readOnly = true)
    public List<SalesPersonResponse> findAll() {
        return salesPersonRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalesPersonResponse findById(Long id) {
        return toResponse(getSalesPerson(id));
    }

    public SalesPersonResponse create(SalesPersonRequest request) {
        SalesPerson salesPerson = new SalesPerson();
        applyRequest(salesPerson, request);
        return toResponse(salesPersonRepository.save(salesPerson));
    }

    public SalesPersonResponse update(Long id, SalesPersonRequest request) {
        SalesPerson salesPerson = getSalesPerson(id);
        applyRequest(salesPerson, request);
        return toResponse(salesPersonRepository.save(salesPerson));
    }

    public void delete(Long id) {
        salesPersonRepository.delete(getSalesPerson(id));
    }

    public SalesPerson getSalesPerson(Long id) {
        return salesPersonRepository.findById(id)
                .orElseThrow(() -> new SalesPersonNotFoundException(id));
    }

    private void applyRequest(SalesPerson salesPerson, SalesPersonRequest request) {
        salesPerson.setSalesPerson(request.salesPerson());
        salesPerson.setSpEmail(request.spEmail());
    }

    private SalesPersonResponse toResponse(SalesPerson salesPerson) {
        return new SalesPersonResponse(salesPerson.getSpId(), salesPerson.getSalesPerson(), salesPerson.getSpEmail());
    }
}