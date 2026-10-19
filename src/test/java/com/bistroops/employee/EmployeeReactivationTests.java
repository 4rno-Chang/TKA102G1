package com.bistroops.employee;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import com.bistroops.employee.controller.EmployeeController;
import com.bistroops.employee.model.*;
import com.bistroops.permission.model.PermissionRepository;

class EmployeeReactivationTests {
    @Test
    void reactivationReplacesPasswordAndAllowsEmailRepairAfterDeliveryFailure() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                mock(PermissionRepository.class), encoder);
        EmployeeMailService mail = mock(EmployeeMailService.class);
        var accounts = new EmployeeAccountService(service, mail);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(3);
        employee.setEmpStatus("2");
        employee.setEmpPwdChanged(true);
        employee.setEmpPassword(encoder.encode("Test1234"));
        when(repository.findForReactivation(3)).thenReturn(Optional.of(employee));
        when(repository.findById(3)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var password = org.mockito.ArgumentCaptor.forClass(String.class);

        var result = accounts.enableEmployee(3);

        assertEquals("1", employee.getEmpStatus());
        assertFalse(employee.getEmpPwdChanged());
        assertTrue(employee.getEmpTempPasswordExpiresAt().isAfter(Instant.now().plusSeconds(86000)));
        assertFalse(result.emailSent());
        assertTrue(result.message().contains("已恢復在職"));
        verify(mail).sendTemporaryPassword(eq(employee), password.capture());
        assertTrue(encoder.matches(password.getValue(), employee.getEmpPassword()));
        assertNull(service.login(3, "Test1234"));
        assertNotNull(service.login(3, password.getValue()));
        var order = inOrder(repository, mail);
        order.verify(repository).save(employee);
        order.verify(mail).sendTemporaryPassword(employee, password.getValue());

        EmployeeVO edited = new EmployeeVO();
        edited.setEmpEmail("repaired@example.com");
        service.updateEmployee(3, edited);
        when(mail.sendTemporaryPassword(eq(employee), anyString())).thenReturn(true);
        assertTrue(accounts.resetPassword(3).emailSent());
        assertEquals("repaired@example.com", employee.getEmpEmail());
    }

    @Test
    void successfulDeliveryDoesNotExposeTemporaryPasswordAndDuplicateEnableDoesNotSend() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        var service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                mock(PermissionRepository.class), PasswordEncoderFactories.createDelegatingPasswordEncoder());
        EmployeeMailService mail = mock(EmployeeMailService.class);
        var accounts = new EmployeeAccountService(service, mail);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(3);
        employee.setEmpStatus("2");
        employee.setEmpEmail("employee@example.com");
        when(repository.findForReactivation(3)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(mail.sendTemporaryPassword(eq(employee), anyString())).thenReturn(true);
        var result = accounts.enableEmployee(3);
        assertTrue(result.emailSent());
        assertEquals(3, result.empNo());
        var password = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).sendTemporaryPassword(eq(employee), password.capture());
        assertFalse(result.toString().contains(password.getValue()));
        String hash = employee.getEmpPassword();
        assertThrows(IllegalStateException.class, () -> accounts.enableEmployee(3));
        assertEquals(hash, employee.getEmpPassword());
        verify(mail, times(1)).sendTemporaryPassword(any(), anyString());
        verify(repository, times(1)).save(any());
    }

    @Test
    void missingEmployeeDoesNotSendMail() {
        EmployeeService service = mock(EmployeeService.class);
        EmployeeMailService mail = mock(EmployeeMailService.class);
        when(service.enableEmployee(eq(99), anyString())).thenThrow(new java.util.NoSuchElementException("找不到員工"));
        assertThrows(java.util.NoSuchElementException.class,
                () -> new EmployeeAccountService(service, mail).enableEmployee(99));
        verifyNoInteractions(mail);
    }

    @Test
    void controllerRequiresPermissionSevenAndReturnsDeliveryAndConflictResults() {
        EmployeeService service = mock(EmployeeService.class);
        EmployeeAccountService accounts = mock(EmployeeAccountService.class);
        EmployeeController controller = new EmployeeController(service, null, accounts);
        MockHttpSession session = new MockHttpSession();
        assertEquals(401, controller.enableEmployee(3, session).getStatusCode().value());
        session.setAttribute("employeeNo", 42);
        assertEquals(403, controller.enableEmployee(3, session).getStatusCode().value());
        verifyNoInteractions(accounts);
        when(service.hasPermission(42, 7)).thenReturn(true);
        when(accounts.enableEmployee(3)).thenThrow(new java.util.NoSuchElementException("找不到員工"));
        assertEquals(404, controller.enableEmployee(3, session).getStatusCode().value());
        var result = new EmployeeAccountService.DeliveryResult(3, false, "已恢復在職，寄信失敗");
        doReturn(result).when(accounts).enableEmployee(3);
        var response = controller.enableEmployee(3, session);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(result, response.getBody());
        when(accounts.enableEmployee(3)).thenThrow(new IllegalStateException("已在職"));
        assertEquals(409, controller.enableEmployee(3, session).getStatusCode().value());
    }

    @Test
    void missingRecipientDoesNotInvokeSmtp() {
        var mail = new EmployeeMailService("smtp.example.com", 587, "test", "test", "sender@example.com", "http://localhost");
        try (var transport = mockStatic(javax.mail.Transport.class)) {
            assertFalse(mail.sendTemporaryPassword(new EmployeeVO(), "Temporary123"));
            transport.verifyNoInteractions();
        }
    }
}
