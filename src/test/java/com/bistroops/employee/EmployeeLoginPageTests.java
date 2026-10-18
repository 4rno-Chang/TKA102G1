package com.bistroops.employee;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import com.bistroops.IndexController_inSpringBoot;
import com.bistroops.employee.controller.EmployeeController;
import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;

class EmployeeLoginPageTests {
    private final EmployeeService service = mock(EmployeeService.class);
    private final IndexController_inSpringBoot controller = new IndexController_inSpringBoot();
    private final MockHttpSession session = new MockHttpSession();
    private final ExtendedModelMap model = new ExtendedModelMap();

    EmployeeLoginPageTests() {
        ReflectionTestUtils.setField(controller, "employeeService", service);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signIn() {
        session.setAttribute("employeeNo", 1);
        session.setAttribute("employeeExpireAt", System.currentTimeMillis() + 60000);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("1", null,
                List.of(new SimpleGrantedAuthority("EMPLOYEE"))));
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        EmployeeVO employee = new EmployeeVO();
        employee.setEmpNo(1);
        employee.setEmpName("陳小心");
        employee.setEmpStatus("1");
        when(service.getEmployeeById(1)).thenReturn(Optional.of(employee));
    }

    @Test
    void signedInEmployeeSeesIdentityWithoutAutomaticLogoutOrRedirect() {
        signIn();
        assertEquals("staff/employee/staff_login", controller.staffLogin(session, model));
        assertEquals("陳小心", model.get("loggedInEmployeeName"));
        assertEquals(1, model.get("loggedInEmployeeNo"));
        assertEquals(1, session.getAttribute("employeeNo"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void visitorSeesNormalLogin() {
        assertEquals("staff/employee/staff_login", controller.staffLogin(session, model));
        assertFalse(model.containsKey("loggedInEmployeeNo"));
        verifyNoInteractions(service);
    }

    @Test
    void expiredSessionShowsLoginAndClearsEmployeeIdentity() {
        signIn();
        session.setAttribute("employeeExpireAt", 0L);
        assertEquals("staff/employee/staff_login", controller.staffLogin(session, model));
        assertNull(session.getAttribute("employeeNo"));
        assertFalse(model.containsKey("loggedInEmployeeNo"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void disabledEmployeeCannotReturnToBackendAsAnActiveEmployee() {
        signIn();
        EmployeeVO disabled = new EmployeeVO();
        disabled.setEmpStatus("2");
        when(service.getEmployeeById(1)).thenReturn(Optional.of(disabled));
        controller.staffLogin(session, model);
        assertNull(session.getAttribute("employeeNo"));
        assertFalse(model.containsKey("loggedInEmployeeNo"));
    }

    @Test
    void confirmedSwitchUsesExistingLogoutAndPreservesUnrelatedMemberSession() {
        signIn();
        session.setAttribute("memNo", 99);
        new EmployeeController(service, null, null).logout(session);
        assertNull(session.getAttribute("employeeNo"));
        assertNull(session.getAttribute("employeeExpireAt"));
        assertEquals(99, session.getAttribute("memNo"));
        assertEquals("staff/employee/staff_login", controller.staffLogin(session, model));
        assertFalse(model.containsKey("loggedInEmployeeNo"));
    }
}
