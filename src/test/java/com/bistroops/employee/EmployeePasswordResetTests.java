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
    void resetUsesStoredPhoneAndRequiresPasswordChangeOnNextLogin() {
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpTel("0912345678");
        employee.setEmpStatus("1");
        employee.setEmpPassword(encoder.encode("oldPassword123"));
        employee.setEmpPwdChanged(true);
        when(repository.findById(2)).thenReturn(Optional.of(employee));

        service.resetPassword(2);

        assertTrue(encoder.matches("0912345678", employee.getEmpPassword()));
        assertFalse(encoder.matches("oldPassword123", employee.getEmpPassword()));
        assertFalse(employee.getEmpPwdChanged());
        assertNotNull(service.login(2, "0912345678"));
        assertNull(service.login(2, "oldPassword123"));
        service.changePassword(2, "newPassword123");
        assertTrue(employee.getEmpPwdChanged());
        assertNotNull(service.login(2, "newPassword123"));
        assertNull(service.login(2, "0912345678"));
    }

    @Test
    void invalidPhoneDoesNotChangePassword() {
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpStatus("1");
        employee.setEmpTel("invalid");
        employee.setEmpPassword("existingHash");
        employee.setEmpPwdChanged(true);
        when(repository.findById(2)).thenReturn(Optional.of(employee));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(2));
        assertEquals("existingHash", employee.getEmpPassword());
        assertTrue(employee.getEmpPwdChanged());
        verify(repository, never()).save(any());
    }

    @Test
    void disabledOrMissingEmployeeCannotBeReset() {
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpStatus("2");
        when(repository.findById(2)).thenReturn(Optional.of(employee));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(2));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(99));
        verify(repository, never()).save(any());
    }

    @Test
    void onlyAuthenticatedManagerCanResetPassword() {
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeeController controller = new EmployeeController(employeeService, null);
        MockHttpSession session = new MockHttpSession();
        assertEquals(401, controller.resetPassword(2, session).getStatusCode().value());
        session.setAttribute("employeeNo", 1);
        assertEquals(403, controller.resetPassword(2, session).getStatusCode().value());
        verify(employeeService, never()).resetPassword(any());

        when(employeeService.hasPermission(1, 7)).thenReturn(true);
        when(employeeService.getEmployeeById(2)).thenReturn(Optional.of(new EmployeeVO()));
        assertEquals(200, controller.resetPassword(2, session).getStatusCode().value());
        verify(employeeService).resetPassword(2);
        assertEquals(404, controller.resetPassword(99, session).getStatusCode().value());
    }
}
