package com.gmsoft.accounts.service.impl;

import com.gmsoft.accounts.dto.AccountsDto;
import com.gmsoft.accounts.dto.CardsDto;
import com.gmsoft.accounts.dto.CustomerDetailsDto;
import com.gmsoft.accounts.dto.LoansDto;
import com.gmsoft.accounts.entity.Accounts;
import com.gmsoft.accounts.entity.Customer;
import com.gmsoft.accounts.exception.ResourceNotFoundException;
import com.gmsoft.accounts.mapper.AccountsMapper;
import com.gmsoft.accounts.mapper.CustomerMapper;
import com.gmsoft.accounts.repository.AccountsRepository;
import com.gmsoft.accounts.repository.CustomerRepository;
import com.gmsoft.accounts.service.ICustomersService;
import com.gmsoft.accounts.service.client.CardsFeignClient;
import com.gmsoft.accounts.service.client.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomersServiceImpl implements ICustomersService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private CardsFeignClient cardsFeignClient;
    private LoansFeignClient loansFeignClient;

    /**
     * @param mobileNumber - Input Mobile Number
     *  @param correlationId - Correlation ID value generated at Edge server
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
        customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());

        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);
        customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());

        return customerDetailsDto;

    }
}
