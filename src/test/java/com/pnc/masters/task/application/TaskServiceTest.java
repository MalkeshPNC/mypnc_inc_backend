package com.pnc.masters.task.application;

import com.pnc.masters.configuration.SubConfigEntry;
import com.pnc.masters.configuration.SubConfigEntryRepository;
import com.pnc.masters.configuration.SubConfigType;
import com.pnc.masters.configuration.SubConfigTypeRepository;
import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.security.EmployeeGroupMemberRepository;
import com.pnc.masters.security.EmployeeGroupRepository;
import com.pnc.masters.task.Task;
import com.pnc.masters.task.TaskAssignee;
import com.pnc.masters.task.TaskAssigneeRepository;
import com.pnc.masters.task.TaskFile;
import com.pnc.masters.task.TaskFileRepository;
import com.pnc.masters.security.EmployeeGroup;
import com.pnc.masters.task.TaskGroupLink;
import com.pnc.masters.task.TaskGroupRepository;
import com.pnc.masters.task.TaskLog;
import com.pnc.masters.task.TaskLogRepository;
import com.pnc.masters.task.TaskRepository;
import com.pnc.masters.task.TaskReviewerRepository;
import com.pnc.masters.task.api.TaskCreateRequest;
import com.pnc.masters.task.api.TaskProgressRequest;
import com.pnc.masters.task.api.TaskUpdateRequest;
import com.pnc.masters.task.api.TaskValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T09:00:00Z");

    @Mock
    private TaskRepository tasks;
    @Mock
    private TaskFileRepository files;
    @Mock
    private TaskAssigneeRepository assignees;
    @Mock
    private TaskReviewerRepository reviewers;
    @Mock
    private TaskGroupRepository taskGroups;
    @Mock
    private AppUserRepository users;
    @Mock
    private EmployeeGroupRepository employeeGroups;
    @Mock
    private EmployeeGroupMemberRepository groupMembers;
    @Mock
    private SubConfigTypeRepository configTypes;
    @Mock
    private SubConfigEntryRepository configEntries;
    @Mock
    private DocumentStorage storage;
    @Mock
    private AppNotifier notifier;
    @Mock
    private TaskLogRepository taskLogs;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(
                tasks,
                files,
                assignees,
                reviewers,
                taskGroups,
                users,
                employeeGroups,
                groupMembers,
                configTypes,
                configEntries,
                storage,
                new DocumentProperties(),
                notifier,
                taskLogs,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createNotifiesEveryNewAssigneeExceptYourself() {
        when(users.findByEnabledTrueOrderByDisplayNameAsc()).thenReturn(List.of(user(9L, "Sam"), user(3L, "Alex")));
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, "Alex")));
        when(users.findAllById(any())).thenAnswer(invocation -> named(invocation.getArgument(0)));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setTaskId(7L);
            return task;
        });

        var response = service.create(3L, new TaskCreateRequest(
                "Check the fixture", "Look at the drawing", null, "MEDIUM", null, null, null, List.of(9L, 3L), List.of(), null, null));

        assertThat(response.status()).isEqualTo(Task.CREATED);
        ArgumentCaptor<TaskAssignee> saved = ArgumentCaptor.forClass(TaskAssignee.class);
        verify(assignees, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(TaskAssignee::getUserId).contains(9L, 3L);
        assertThat(saved.getAllValues()).allMatch(row -> Task.CREATED.equals(row.getStatus()));
        verify(notifier).notifyUsers(
                eq(List.of(9L)),
                eq("Task assigned"),
                eq("Task assigned"),
                eq("Alex assigned this task to you."),
                eq("/tasks"));
        verify(notifier, never()).notifyUsers(eq(List.of(3L)), any(), any(), any(), any());
    }

    @Test
    void developmentAssigneesHaveNoStatus() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(1L);
        type.setTypeName("Task types");
        SubConfigEntry entry = new SubConfigEntry();
        entry.setFieldValue(1, "DEVELOPMENT");
        when(configTypes.findAllByOrderByTypeNameAsc()).thenReturn(List.of(type));
        when(configEntries.findByTypeTypeIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entry));
        when(users.findByEnabledTrueOrderByDisplayNameAsc()).thenReturn(List.of(user(9L, "Sam")));
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, "Alex")));
        when(users.findAllById(any())).thenAnswer(invocation -> named(invocation.getArgument(0)));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setTaskId(7L);
            return task;
        });

        var response = service.create(3L, new TaskCreateRequest(
                "Build the screen", null, "DEVELOPMENT", "HIGH", "J-1", "NC-9", null, List.of(9L), null, null, null));

        assertThat(response.taskType()).isEqualTo("DEVELOPMENT");
        assertThat(response.jobNumber()).isEqualTo("J-1");
        ArgumentCaptor<TaskAssignee> saved = ArgumentCaptor.forClass(TaskAssignee.class);
        verify(assignees).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isNull();
        assertThat(saved.getValue().getUserId()).isEqualTo(9L);
    }

    @Test
    void leavingDevelopmentKeepsEachPersonOnTheTaskStatus() {
        Task existing = task(7L, "Build");
        existing.setTaskType("DEVELOPMENT");
        existing.setStatus(Task.IN_PROCESS);
        TaskAssignee row = new TaskAssignee();
        row.setTaskId(7L);
        row.setUserId(9L);
        SubConfigType type = new SubConfigType();
        type.setTypeId(1L);
        type.setTypeName("Task types");
        SubConfigEntry entry = new SubConfigEntry();
        entry.setFieldValue(1, "Quality");
        when(configTypes.findAllByOrderByTypeNameAsc()).thenReturn(List.of(type));
        when(configEntries.findByTypeTypeIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(entry));
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        when(assignees.findByTaskIdOrderByUserIdAsc(7L)).thenReturn(List.of(row));
        when(users.findAllById(any())).thenReturn(List.of());

        service.update(7L, 3L, new TaskUpdateRequest(
                null, null, "Quality", null, null, null, null, null, false, null, false, null, false, null, null, null, false));

        assertThat(existing.getStatus()).isEqualTo(Task.IN_PROCESS);
        assertThat(row.getStatus()).isEqualTo(Task.IN_PROCESS);
    }

    @Test
    void statusOnlyUpdateKeepsTheTitle() {
        Task existing = task(7L, "Keep me");
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        when(users.findAllById(any())).thenReturn(List.of());

        var response = service.update(7L, 3L, new TaskUpdateRequest(
                null, null, null, null, null, null, "in_process", null, false, null, false, null, false, null, null, null, false));

        assertThat(response.title()).isEqualTo("Keep me");
        assertThat(response.status()).isEqualTo(Task.IN_PROCESS);
        verify(assignees, never()).deleteByTaskId(any());
    }

    @Test
    void onlyTheCreatorCanMoveADevelopmentTask() {
        Task existing = task(7L, "Build");
        existing.setTaskType("DEVELOPMENT");
        existing.setCreatedBy(3L);
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(7L, 9L, new TaskUpdateRequest(
                null, null, null, null, null, null, Task.REVIEW, null, false, null, false, null, false, null, null, null, false)))
                .isInstanceOf(TaskValidationException.class)
                .hasMessage("Only the person who created this task can change it.");
    }

    @Test
    void anAssigneeCanUpdateTheirOwnStatusAndComment() {
        Task existing = task(7L, "Check");
        TaskAssignee row = new TaskAssignee();
        row.setTaskId(7L);
        row.setUserId(9L);
        row.setStatus(Task.CREATED);
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        when(assignees.findByTaskIdAndUserId(7L, 9L)).thenReturn(Optional.of(row));
        when(users.findAllById(any())).thenReturn(List.of());

        service.progress(7L, 9L, new TaskProgressRequest(Task.IN_PROCESS, "Started", true));

        assertThat(row.getStatus()).isEqualTo(Task.IN_PROCESS);
        assertThat(row.getComment()).isEqualTo("Started");
        assertThat(row.getCommentAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));

        service.progress(7L, 9L, new TaskProgressRequest(Task.IN_PROCESS, "Started", true));

        assertThat(row.getCommentAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ArgumentCaptor<TaskLog> logged = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogs, times(1)).save(logged.capture());
        assertThat(logged.getValue().getFieldName()).isEqualTo("Status");
        assertThat(logged.getValue().getOldValue()).isEqualTo("Created");
        assertThat(logged.getValue().getNewValue()).isEqualTo("In Process");
        assertThat(logged.getValue().getUserId()).isEqualTo(9L);
    }

    @Test
    void reminderDateIsStoredAndLogged() {
        Task existing = task(7L, "Keep me");
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        when(users.findAllById(any())).thenReturn(List.of());

        var response = service.update(7L, 3L, new TaskUpdateRequest(
                null, null, null, null, null, null, null, null, false, null, false, null, false, null, "2026-11-02", null, false));

        assertThat(response.reminderDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        ArgumentCaptor<TaskLog> logged = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogs).save(logged.capture());
        assertThat(logged.getValue().getFieldName()).isEqualTo("Reminder date");
        assertThat(logged.getValue().getOldValue()).isNull();
        assertThat(logged.getValue().getNewValue()).isEqualTo("2 Nov 2026");
    }

    @Test
    void reminderNotifiesTheCreatorAndAssigneesOnce() {
        Task existing = task(7L, "Check the fixture");
        existing.setReminderDate(LocalDate.of(2026, 10, 26));
        when(tasks.findRemindersDue(LocalDate.of(2026, 10, 26))).thenReturn(List.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        TaskAssignee assignee = new TaskAssignee();
        assignee.setTaskId(7L);
        assignee.setUserId(9L);
        when(assignees.findByTaskIdIn(List.of(7L))).thenReturn(List.of(assignee));

        service.sendDueReminders();
        service.sendDueReminders();

        verify(notifier, times(1)).notifyUsers(
                eq(List.of(3L, 9L)),
                eq("task-reminder"),
                eq("Task reminder"),
                eq("Check the fixture is due 26 Oct 2026."),
                eq("/tasks"));
        assertThat(existing.getReminderNotifiedOn()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void jobGroupAndNcChangesAreLogged() {
        Task existing = task(7L, "Keep me");
        existing.setJobNumber("J-1");
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        when(users.findAllById(any())).thenReturn(List.of());
        TaskGroupLink qualityLink = new TaskGroupLink();
        qualityLink.setGroupId(1L);
        TaskGroupLink shopLink = new TaskGroupLink();
        shopLink.setGroupId(2L);
        when(taskGroups.findByTaskId(7L)).thenReturn(List.of(qualityLink), List.of(shopLink));
        EmployeeGroup shop = new EmployeeGroup();
        shop.setGroupId(2L);
        shop.setName("Shop");
        when(employeeGroups.findByDeletedFalseAndTypeIgnoreCaseOrderByNameAsc("Task Management")).thenReturn(List.of(shop));
        when(employeeGroups.findAllById(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> {
                EmployeeGroup group = new EmployeeGroup();
                group.setGroupId(id);
                group.setName(id == 1L ? "Quality" : "Shop");
                return group;
            }).toList();
        });

        service.update(7L, 3L, new TaskUpdateRequest(
                null, null, null, null, "J-2", "NC-9", null, null, false, List.of(2L), true, null, false, null, null, null, false));

        ArgumentCaptor<TaskLog> logged = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogs, times(3)).save(logged.capture());
        assertThat(logged.getAllValues())
                .extracting(TaskLog::getFieldName, TaskLog::getOldValue, TaskLog::getNewValue)
                .containsExactly(
                        tuple("Job number", "J-1", "J-2"),
                        tuple("NC#", null, "NC-9"),
                        tuple("Employee group", "Quality", "Shop"));
    }

    @Test
    void anAssigneeOnADevelopmentTaskCanCommentButNotSetStatus() {
        Task existing = task(7L, "Build");
        existing.setTaskType("DEVELOPMENT");
        TaskAssignee row = new TaskAssignee();
        row.setTaskId(7L);
        row.setUserId(9L);
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(tasks.save(existing)).thenReturn(existing);
        when(assignees.findByTaskIdAndUserId(7L, 9L)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.progress(7L, 9L, new TaskProgressRequest(Task.DONE, "Noted", true)))
                .isInstanceOf(TaskValidationException.class);

        service.progress(7L, 9L, new TaskProgressRequest(null, "Looks good", true));

        assertThat(row.getComment()).isEqualTo("Looks good");
        assertThat(row.getStatus()).isNull();
    }

    @Test
    void rejectsAnUnknownStatus() {
        when(tasks.findById(7L)).thenReturn(Optional.of(task(7L, "Keep me")));

        assertThatThrownBy(() -> service.update(7L, 3L, new TaskUpdateRequest(
                null, null, null, null, null, null, "LATER", null, false, null, false, null, false, null, null, null, false)))
                .isInstanceOf(TaskValidationException.class);
        verify(tasks, never()).save(any());
    }

    @Test
    void rejectsAnAssigneeWhoIsNotEnabled() {
        when(users.findByEnabledTrueOrderByDisplayNameAsc()).thenReturn(List.of(user(3L, "Alex")));
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setTaskId(7L);
            return task;
        });

        assertThatThrownBy(() -> service.create(3L, new TaskCreateRequest(
                "Check the fixture", null, null, null, null, null, null, List.of(9L), null, null, null)))
                .isInstanceOf(TaskValidationException.class)
                .hasMessage("Choose a person from the list");
    }

    @Test
    void creatorCanDeleteAFileWhileTheTaskIsCreated() throws Exception {
        Task existing = task(7L, "Keep me");
        TaskFile file = new TaskFile();
        file.setFileId(4L);
        file.setTaskId(7L);
        file.setStorageKey("task/7/4-drawing.pdf");
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));
        when(files.findById(4L)).thenReturn(Optional.of(file));
        when(assignees.findByTaskIdOrderByUserIdAsc(7L)).thenReturn(List.of());

        service.deleteFile(7L, 3L, 4L);

        verify(storage).delete("task/7/4-drawing.pdf");
        verify(files).delete(file);
    }

    @Test
    void filesStayOnceTheTaskLeavesCreated() {
        Task existing = task(7L, "Keep me");
        existing.setStatus(Task.IN_PROCESS);
        when(tasks.findById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteFile(7L, 3L, 4L))
                .isInstanceOf(TaskValidationException.class)
                .hasMessage("Files can only be removed while the task is Created.");
        verify(files, never()).delete(any());
    }

    @Test
    void rejectsAFileOverTenMegabytes() {
        when(tasks.findById(7L)).thenReturn(Optional.of(task(7L, "Keep me")));
        MockMultipartFile oversized = new MockMultipartFile("file", "drawing.pdf", "application/pdf", new byte[] {1}) {
            @Override
            public long getSize() {
                return 10L * 1024 * 1024 + 1;
            }
        };

        assertThatThrownBy(() -> service.addFile(7L, 3L, oversized))
                .isInstanceOf(TaskValidationException.class)
                .hasMessage("The file is larger than 10 MB.");
    }

    @Test
    void peopleAreEveryEnabledUser() {
        when(users.findByEnabledTrueOrderByDisplayNameAsc()).thenReturn(List.of(user(9L, "Sam"), user(3L, "Alex")));

        assertThat(service.people()).extracting(person -> person.displayName()).containsExactly("Sam", "Alex");
    }

    private static Task task(Long id, String title) {
        Task task = new Task();
        task.setTaskId(id);
        task.setTitle(title);
        task.setStatus(Task.CREATED);
        task.setCreatedBy(3L);
        task.setCreatedAt(LocalDateTime.of(2026, 10, 26, 8, 0));
        return task;
    }

    private static List<AppUser> named(Collection<Long> ids) {
        return ids.stream().map(id -> user(id, id == 3L ? "Alex" : "Sam")).toList();
    }

    private static AppUser user(Long id, String name) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setDisplayName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setEnabled(true);
        return user;
    }
}
