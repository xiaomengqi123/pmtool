package com.pmtool;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerDetailsControllerTest {
    @Test
    void customerDetailsReadsRequireManagerRole() {
        PmToolService service = mock(PmToolService.class);
        CustomerContactRepository contacts = mock(CustomerContactRepository.class);
        CustomerFollowUpRepository followUps = mock(CustomerFollowUpRepository.class);
        Customer customer = new Customer();
        customer.id = 1L;
        when(service.customer(1L)).thenReturn(customer);
        when(contacts.findByCustomerIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(followUps.findByCustomerIdAndDeletedFalseOrderByFollowUpAtDesc(1L)).thenReturn(List.of());
        CustomerDetailsController controller = new CustomerDetailsController(service, contacts, followUps);

        controller.contacts(1L);
        controller.followUps(1L);

        verify(service, org.mockito.Mockito.times(2)).requireManager();
    }
}
