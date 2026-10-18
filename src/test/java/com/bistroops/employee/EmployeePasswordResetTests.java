package com.bistroops.employee;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.bistroops.employee.controller.EmployeeController;
import com.bistroops.employee.model.*;

class EmployeePasswordResetTests {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final EmployeeService service = new EmployeeService(repository,
            mock(EmployeePermissionRepository.class),
            mock(com.bistroops.permission.model.PermissionRepository.class), encoder);

    @Test
    void resetUsesTemporaryPasswordAndRequiresPasswordChangeOnNextLogin() {
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpTel("0912345678");
        employee.setEmpEmail("employee@example.com");
        employee.setEmpStatus("1");
        employee.setEmpPassword(encoder.encode("oldPassword123"));
        employee.setEmpPwdChanged(true);
        when(repository.findById(2)).thenReturn(Optional.of(employee));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetPassword(2, "TempPass12345");

        assertTrue(encoder.matches("TempPass12345", employee.getEmpPassword()));
        assertFalse(encoder.matches("oldPassword123", employee.getEmpPassword()));
        assertFalse(employee.getEmpPwdChanged());
        assertNotNull(service.login(2, "TempPass12345"));
        assertNull(service.login(2, "oldPassword123"));
        service.changePassword(2, "newPassword123");
        assertTrue(employee.getEmpPwdChanged());
        assertNotNull(service.login(2, "newPassword123"));
        assertNull(service.login(2, "TempPass12345"));
    }

    @Test
    void invalidEmailDoesNotChangePassword() {
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpStatus("1");
        employee.setEmpEmail("invalid");
        employee.setEmpPassword("existingHash");
        employee.setEmpPwdChanged(true);
        when(repository.findById(2)).thenReturn(Optional.of(employee));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(2, "TempPass12345"));
        assertEquals("existingHash", employee.getEmpPassword());
        assertTrue(employee.getEmpPwdChanged());
        verify(repository, never()).save(any());
    }

    @Test
    void disabledOrMissingEmployeeCannotBeReset() {
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpStatus("2");
        when(repository.findById(2)).thenReturn(Optional.of(employee));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(2, "TempPass12345"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(99, "TempPass12345"));
        verify(repository, never()).save(any());
    }

    @Test
    void onlyAuthenticatedManagerCanResetPassword() {
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeeAccountService accounts = mock(EmployeeAccountService.class);
        EmployeeController controller = new EmployeeController(employeeService, null, accounts);
        MockHttpSession session = new MockHttpSession();
        assertEquals(401, controller.resetPassword(2, session).getStatusCode().value());
        session.setAttribute("employeeNo", 1);
        assertEquals(403, controller.resetPassword(2, session).getStatusCode().value());
        verify(accounts, never()).resetPassword(any());

        when(employeeService.hasPermission(1, 7)).thenReturn(true);
        when(employeeService.getEmployeeById(2)).thenReturn(Optional.of(new EmployeeVO()));
        assertEquals(200, controller.resetPassword(2, session).getStatusCode().value());
        verify(accounts).resetPassword(2);
        assertEquals(404, controller.resetPassword(99, session).getStatusCode().value());
    }
}
