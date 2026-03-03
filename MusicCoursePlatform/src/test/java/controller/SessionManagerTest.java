package controller;

import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @BeforeEach
    void setUp() {
        // Reset singleton state before each test
        SessionManager.getInstance().logout();
    }

    @Test
    void testGetInstance_ReturnsSameInstance() {
        SessionManager instance1 = SessionManager.getInstance();
        SessionManager instance2 = SessionManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testIsLoggedIn_WhenNoUser_ReturnsFalse() {
        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    @Test
    void testIsLoggedIn_WhenUserSet_ReturnsTrue() {
        SessionManager.getInstance().setCurrentUser(createUser("LEARNER"));
        assertTrue(SessionManager.getInstance().isLoggedIn());
    }

    @Test
    void testGetCurrentUser_WhenNoUser_ReturnsNull() {
        assertNull(SessionManager.getInstance().getCurrentUser());
    }

    @Test
    void testGetCurrentUser_WhenUserSet_ReturnsUser() {
        User user = createUser("LEARNER");
        SessionManager.getInstance().setCurrentUser(user);
        assertEquals(user, SessionManager.getInstance().getCurrentUser());
    }

    @Test
    void testIsTeacher_WhenTeacher_ReturnsTrue() {
        SessionManager.getInstance().setCurrentUser(createUser("TEACHER"));
        assertTrue(SessionManager.getInstance().isTeacher());
    }

    @Test
    void testIsTeacher_WhenLearner_ReturnsFalse() {
        SessionManager.getInstance().setCurrentUser(createUser("LEARNER"));
        assertFalse(SessionManager.getInstance().isTeacher());
    }

    @Test
    void testIsTeacher_WhenNoUser_ReturnsFalse() {
        assertFalse(SessionManager.getInstance().isTeacher());
    }

    @Test
    void testIsLearner_WhenLearner_ReturnsTrue() {
        SessionManager.getInstance().setCurrentUser(createUser("LEARNER"));
        assertTrue(SessionManager.getInstance().isLearner());
    }

    @Test
    void testIsLearner_WhenTeacher_ReturnsFalse() {
        SessionManager.getInstance().setCurrentUser(createUser("TEACHER"));
        assertFalse(SessionManager.getInstance().isLearner());
    }

    @Test
    void testIsLearner_WhenNoUser_ReturnsFalse() {
        assertFalse(SessionManager.getInstance().isLearner());
    }

    @Test
    void testLogout_ClearsCurrentUser() {
        SessionManager.getInstance().setCurrentUser(createUser("LEARNER"));
        SessionManager.getInstance().logout();
        assertNull(SessionManager.getInstance().getCurrentUser());
        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    @Test
    void testGetCurrentUserId_WhenUserSet_ReturnsUserId() {
        User user = createUser("LEARNER");
        SessionManager.getInstance().setCurrentUser(user);
        assertEquals(user.getUserId(), SessionManager.getInstance().getCurrentUserId());
    }

    @Test
    void testGetCurrentUserId_WhenNoUser_ReturnsMinusOne() {
        assertEquals(-1, SessionManager.getInstance().getCurrentUserId());
    }

    // --- Helper ---

    private User createUser(String role) {
        User user = new User();
        user.setUserId(1);
        user.setEmail("test@test.com");
        user.setUserType(role);
        return user;
    }
}