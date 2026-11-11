package mn.edu.must.sqat;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {
    private static final String STUDENT_ID = "B232270144";
    private static final String COURSE_ID = "CSA313";
    private static final String EMAIL = "B232270144@must.edu.mn";

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // A1: The existing prerequisite is completed; saving precedes notification.
    @Test
    void register_activeStudentWithPrerequisites_returnsOkAndSavesAndEmails() {
        stubEligibleStudent();

        assertEquals("OK", service.register(STUDENT_ID, COURSE_ID));

        InOrder order = inOrder(repository, emailSender);
        order.verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        order.verify(emailSender).send(EMAIL, "Бүртгэл баталгаажлаа",
                COURSE_ID + " хичээлд амжилттай бүртгэгдлээ.");
    }

    // A2
    @Test
    void register_missingStudent_returnsNoStudentWithoutSavingOrEmailing() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        assertEquals("ERROR_NO_STUDENT", service.register(STUDENT_ID, COURSE_ID));

        verify(repository, never()).saveRegistration(any(), any());
        verify(emailSender, never()).send(any(), any(), any());
    }

    // A3
    @Test
    void register_inactiveStudent_returnsInactiveWithoutEmailInteractions() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, EMAIL, false, List.of()));

        assertEquals("ERROR_INACTIVE_STUDENT", service.register(STUDENT_ID, COURSE_ID));

        verify(repository, never()).saveRegistration(any(), any());
        verifyNoInteractions(emailSender);
    }

    // A4: One completed prerequisite must not hide the other missing prerequisite.
    @Test
    void register_missingPrerequisite_returnsPrerequisitesWithoutSaving() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, EMAIL, true, List.of("CSA211")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA211", "CSA212"));

        assertEquals("ERROR_PREREQUISITES", service.register(STUDENT_ID, COURSE_ID));

        verify(repository, never()).saveRegistration(any(), any());
        verifyNoInteractions(emailSender);
    }

    // A5
    @Test
    void register_emailSenderThrows_returnsOkAndKeepsRegistration() {
        stubEligibleStudent();
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(eq(EMAIL), anyString(), contains(COURSE_ID));

        assertEquals("OK", service.register(STUDENT_ID, COURSE_ID));

        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), eq("Бүртгэл баталгаажлаа"), contains(COURSE_ID));
    }

    // A6: Each argument is tested separately, so removing either null guard is detected.
    @Test
    void register_eitherOrBothIdsNull_throwsWithoutDependencyInteractions() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.register(null, COURSE_ID)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.register(STUDENT_ID, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.register(null, null)));
        verifyNoInteractions(repository, emailSender);
    }

    // A7: Use a real Student value object and capture only the outgoing message.
    @Test
    void register_successfulRegistration_emailBodyContainsCourseCode() {
        stubEligibleStudent();
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        assertEquals("OK", service.register(STUDENT_ID, COURSE_ID));

        verify(emailSender).send(eq(EMAIL), eq("Бүртгэл баталгаажлаа"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains(COURSE_ID));
        assertEquals(COURSE_ID + " хичээлд амжилттай бүртгэгдлээ.", bodyCaptor.getValue());
    }

    // A8: Create only stubs that the selected error path actually uses.
    @ParameterizedTest(name = "A8 {index}: {0} -> {1}")
    @CsvSource({
            "MISSING_STUDENT, ERROR_NO_STUDENT",
            "INACTIVE_STUDENT, ERROR_INACTIVE_STUDENT",
            "MISSING_COURSE, ERROR_NO_COURSE"
    })
    void register_invalidStudentOrCourse_returnsExpectedErrorWithoutSideEffects(
            String scenario, String expected) {
        switch (scenario) {
            case "MISSING_STUDENT" -> when(repository.findStudent(STUDENT_ID)).thenReturn(null);
            case "INACTIVE_STUDENT" -> when(repository.findStudent(STUDENT_ID))
                    .thenReturn(new Student(STUDENT_ID, EMAIL, false, List.of()));
            case "MISSING_COURSE" -> {
                when(repository.findStudent(STUDENT_ID))
                        .thenReturn(new Student(STUDENT_ID, EMAIL, true, List.of()));
                when(repository.courseExists(COURSE_ID)).thenReturn(false);
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }

        assertEquals(expected, service.register(STUDENT_ID, COURSE_ID));

        verify(repository, never()).saveRegistration(any(), any());
        verifyNoInteractions(emailSender);
    }

    private void stubEligibleStudent() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, EMAIL, true, List.of("CSA211", "CSA212")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA211"));
    }
}
