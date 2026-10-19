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
    void invalidInitialPermissionsNeverCreateEmployeeOrSendMail() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        var permissions = mock(com.bistroops.permission.model.PermissionRepository.class);
        EmployeeService service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                permissions, PasswordEncoderFactories.createDelegatingPasswordEncoder());
        EmployeeMailService mail = mock(EmployeeMailService.class);
        EmployeeAccountService accounts = new EmployeeAccountService(service, mail);
        for (java.util.List<Integer> selection : java.util.Arrays.asList(null,
                java.util.List.<Integer>of(), java.util.List.of(8), java.util.Arrays.asList(1, null),
                java.util.List.of(1))) {
            assertThrows(IllegalArgumentException.class, () -> accounts.createEmployee(new EmployeeVO(), selection));
        }
        verifyNoInteractions(repository, mail);
    }

    @Test
    void initialPermissionsAreAssignedBeforeMailAndDuplicatesAreIgnored() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        var permissionRepository = mock(com.bistroops.permission.model.PermissionRepository.class);
        EmployeeService service = new EmployeeService(repository, mock(EmployeePermissionRepository.class),
                permissionRepository, PasswordEncoderFactories.createDelegatingPasswordEncoder());
        var permission = new com.bistroops.permission.model.PermissionVO();
        permission.setPermNo(3);
        when(permissionRepository.findAllById(java.util.Set.of(3))).thenReturn(java.util.List.of(permission));
        when(repository.save(any())).thenAnswer(invocation -> {
            EmployeeVO employee = invocation.getArgument(0);
            employee.setEmpNo(9);
            return employee;
        });
        EmployeeMailService mail = mock(EmployeeMailService.class);
        when(mail.sendTemporaryPassword(any(), anyString())).thenAnswer(invocation -> {
            EmployeeVO saved = invocation.getArgument(0);
            assertEquals(1, saved.getEmployeepermissions().size());
            var assigned = saved.getEmployeepermissions().iterator().next();
            assertEquals(3, assigned.getIds().getPermNo());
            assertEquals(9, assigned.getIds().getEmpNo());
            assertSame(saved, assigned.getEmployee());
            return true;
        });
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpTel("0912345678");
        employee.setEmpEmail("employee@example.com");
        assertTrue(new EmployeeAccountService(service, mail)
                .createEmployee(employee, java.util.List.of(3, 3)).emailSent());
        var order = inOrder(repository, mail);
        order.verify(repository, times(2)).save(employee);
        order.verify(mail).sendTemporaryPassword(eq(employee), anyString());
    }

    @Test
    void creationRequestReadsExistingFieldsAndInitialPermissions() throws Exception {
        var mapper = new tools.jackson.databind.json.JsonMapper();
        var request = mapper.readValue("{\"empName\":\"測試\",\"empTel\":\"0912345678\",\"permissions\":[1,3]}",
                com.bistroops.employee.controller.EmployeeController.CreateEmployeeRequest.class);
        assertEquals("測試", request.employee.getEmpName());
        assertEquals(java.util.List.of(1, 3), request.permissions);
    }

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
        when(service.addEmployeeWithPermissions(eq(employee), anyString(), eq(java.util.List.of(1, 3)))).thenReturn(employee);
        when(mail.sendTemporaryPassword(eq(employee), anyString())).thenReturn(true);
        var result = accounts.createEmployee(employee, java.util.List.of(1, 3));
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(service).addEmployeeWithPermissions(eq(employee), password.capture(), eq(java.util.List.of(1, 3)));
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
        when(service.addEmployeeWithPermissions(eq(employee), anyString(), eq(java.util.List.of(1, 3)))).thenReturn(employee);
        when(service.resetPassword(eq(7), anyString())).thenReturn(employee);
        var failed = accounts.createEmployee(employee, java.util.List.of(1, 3));
        assertFalse(failed.emailSent());
        assertTrue(failed.message().contains("員工已新增"));
        assertTrue(failed.message().contains("忘記密碼"));
        when(mail.sendTemporaryPassword(eq(employee), anyString())).thenReturn(true);
        assertTrue(accounts.resetPassword(7).emailSent());
        verify(service, times(1)).addEmployeeWithPermissions(eq(employee), anyString(), eq(java.util.List.of(1, 3)));
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
