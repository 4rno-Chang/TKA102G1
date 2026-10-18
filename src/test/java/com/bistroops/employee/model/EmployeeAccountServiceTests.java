package com.bistroops.employee.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

class EmployeeAccountServiceTests {
    @Test
    void gmailDefaultsSendFromConfiguredAccountWithoutNetworkAccess() throws Exception {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    context, "employee.mail.username=sender@example.com",
                    "employee.mail.password=abcd efgh ijkl mnop");
            context.register(EmployeeMailService.class);
            context.refresh();
            EmployeeMailService mail = context.getBean(EmployeeMailService.class);
            EmployeeVO employee = new EmployeeVO();
            employee.setEmpNo(7);
            employee.setEmpName("測試員工");
            employee.setEmpEmail("recipient@example.com");
            try (var transport = mockStatic(javax.mail.Transport.class)) {
                transport.when(() -> javax.mail.Transport.send(any(javax.mail.Message.class)))
                        .thenAnswer(invocation -> {
                            javax.mail.internet.MimeMessage message = invocation.getArgument(0);
                            assertEquals("sender@example.com", message.getFrom()[0].toString());
                            assertEquals("smtp.gmail.com", message.getSession().getProperty("mail.smtp.host"));
                            assertEquals("587", message.getSession().getProperty("mail.smtp.port"));
                            assertEquals("true", message.getSession().getProperty("mail.smtp.starttls.required"));
                            assertEquals("recipient@example.com", message.getAllRecipients()[0].toString());
                            return null;
                        });
                assertTrue(mail.sendTemporaryPassword(employee, "TempPass12345"));
                transport.verify(() -> javax.mail.Transport.send(any(javax.mail.Message.class)));
            }
        }
    }

    @Test
    void createsRandomPasswordAndReturnsNoPlaintextToBrowser() {
        EmployeeService service = mock(EmployeeService.class);
        EmployeeMailService mail = mock(EmployeeMailService.class);
        EmployeeAccountService accounts = new EmployeeAccountService(service, mail);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(7);
        employee.setEmpEmail("employee@example.com");
        when(service.addEmployee(eq(employee), anyString())).thenReturn(employee);
        when(mail.sendTemporaryPassword(eq(employee), anyString())).thenReturn(true);
        var result = accounts.createEmployee(employee);
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(service).addEmployee(eq(employee), password.capture());
        verify(mail).sendTemporaryPassword(employee, password.getValue());
        assertTrue(password.getValue().matches("^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{12}$"));
        assertEquals(7, result.empNo());
        assertTrue(result.emailSent());
        assertFalse(result.toString().contains(password.getValue()));
        var generated = new HashSet<String>();
        for (int i = 0; i < 100; i++) generated.add(accounts.generateTemporaryPassword());
        assertEquals(100, generated.size());
    }

    @Test
    void failedMailReportsAccountAlreadyCreatedAndAllowsResetRetry() {
        EmployeeService service = mock(EmployeeService.class);
        EmployeeMailService mail = mock(EmployeeMailService.class);
        EmployeeAccountService accounts = new EmployeeAccountService(service, mail);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(7);
        when(service.addEmployee(eq(employee), anyString())).thenReturn(employee);
        when(service.resetPassword(eq(7), anyString())).thenReturn(employee);
        var failed = accounts.createEmployee(employee);
        assertFalse(failed.emailSent());
        assertTrue(failed.message().contains("員工已新增"));
        assertTrue(failed.message().contains("忘記密碼"));
        when(mail.sendTemporaryPassword(eq(employee), anyString())).thenReturn(true);
        assertTrue(accounts.resetPassword(7).emailSent());
        verify(service, times(1)).addEmployee(eq(employee), anyString());
    }

    @Test
    void expiryIsEnforcedButExistingSeedPasswordsStillWork() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        EmployeeService service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                mock(com.bistroops.permission.model.PermissionRepository.class), encoder);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(1);
        employee.setEmpStatus("1");
        // The exact seed hash supplied in the manual migration instructions.
        employee.setEmpPassword("{bcrypt}$2a$10$g2db.AEZVThNjtzx7H4Wn.yZ8Hd1.M3T6FtcxfVuEK5XR7sTFPjNK");
        when(repository.findById(1)).thenReturn(Optional.of(employee));
        assertNotNull(service.login(1, "Test1234"));
        employee.setEmpTempPasswordExpiresAt(Instant.now().minusSeconds(1));
        assertNull(service.login(1, "Test1234"));
        employee.setEmpTempPasswordExpiresAt(Instant.now().plusSeconds(3600));
        assertNotNull(service.login(1, "Test1234"));
    }

    @Test
    void createPersistsEmailHashAndExpiry() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        EmployeeService service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                mock(com.bistroops.permission.model.PermissionRepository.class), encoder);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpTel("0912345678");
        employee.setEmpEmail(" employee@example.com ");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.addEmployee(employee, "TempPass12345");
        assertEquals("employee@example.com", employee.getEmpEmail());
        assertTrue(employee.getEmpPassword().startsWith("{bcrypt}"));
        assertTrue(encoder.matches("TempPass12345", employee.getEmpPassword()));
        assertFalse(encoder.matches("0912345678", employee.getEmpPassword()));
        assertFalse(employee.getEmpPwdChanged());
        assertTrue(employee.getEmpTempPasswordExpiresAt().isAfter(Instant.now().plusSeconds(86000)));
    }

    @Test
    void updatingEmailDoesNotResetPassword() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        EmployeeService service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                mock(com.bistroops.permission.model.PermissionRepository.class),
                PasswordEncoderFactories.createDelegatingPasswordEncoder());
        EmployeeVO existing = new EmployeeVO();
        existing.setEmpStatus("1");
        existing.setEmpPassword("existingHash");
        existing.setEmpPwdChanged(true);
        when(repository.findById(1)).thenReturn(Optional.of(existing));
        EmployeeVO changed = new EmployeeVO();
        changed.setEmpEmail("new@example.com");
        service.updateEmployee(1, changed);
        assertEquals("new@example.com", existing.getEmpEmail());
        assertEquals("existingHash", existing.getEmpPassword());
        assertTrue(existing.getEmpPwdChanged());
    }

    @Test
    void mailContainsLoginInstructionsAndMissingConfigurationDoesNotSend() {
        EmployeeMailService mail = new EmployeeMailService("", 587, "", "", "",
                "http://localhost:8080/staff/login");
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(7);
        employee.setEmpName("測試員工");
        employee.setEmpEmail("employee@example.com");
        String body = mail.buildBody(employee, "TempPass12345");
        assertTrue(body.contains("http://localhost:8080/staff/login"));
        assertTrue(body.contains("員工帳號：7"));
        assertTrue(body.contains("TempPass12345"));
        assertTrue(body.contains("24 小時"));
        assertFalse(mail.sendTemporaryPassword(employee, "TempPass12345"));
    }
}
