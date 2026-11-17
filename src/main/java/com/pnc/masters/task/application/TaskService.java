package com.pnc.masters.task.application;

import com.pnc.masters.configuration.SubConfigEntryRepository;
import com.pnc.masters.configuration.SubConfigTypeRepository;
import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.notification.AppNotifier;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.security.EmployeeGroup;
import com.pnc.masters.security.EmployeeGroupMember;
import com.pnc.masters.security.EmployeeGroupMemberRepository;
import com.pnc.masters.security.EmployeeGroupRepository;
import com.pnc.masters.task.Task;
import com.pnc.masters.task.TaskAssignee;
import com.pnc.masters.task.TaskAssigneeRepository;
import com.pnc.masters.task.TaskFile;
import com.pnc.masters.task.TaskFileRepository;
import com.pnc.masters.task.TaskGroupLink;
import com.pnc.masters.task.TaskGroupRepository;
import com.pnc.masters.task.TaskLog;
import com.pnc.masters.task.TaskLogRepository;
import com.pnc.masters.task.TaskRepository;
import com.pnc.masters.task.TaskReviewer;
import com.pnc.masters.task.TaskReviewerRepository;
import com.pnc.masters.task.api.TaskAssigneeResponse;
import com.pnc.masters.task.api.TaskLogResponse;
import com.pnc.masters.task.api.TaskCreateRequest;
import com.pnc.masters.task.api.TaskFileResponse;
import com.pnc.masters.task.api.TaskGroupOption;
import com.pnc.masters.task.api.TaskGroupResponse;
import com.pnc.masters.task.api.TaskNotFoundException;
import com.pnc.masters.task.api.TaskOptionsResponse;
import com.pnc.masters.task.api.TaskPersonResponse;
import com.pnc.masters.task.api.TaskReviewerResponse;
import com.pnc.masters.task.api.TaskProgressRequest;
import com.pnc.masters.task.api.TaskResponse;
import com.pnc.masters.task.api.TaskUpdateRequest;
import com.pnc.masters.task.api.TaskValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class TaskService {

    static final String TASK_TYPES_NAME = "Task types";
    static final String TASK_MANAGEMENT_GROUP = "Task Management";
    static final String DEVELOPMENT = "DEVELOPMENT";
    static final String QUOTE_REVIEWER = "QUOTE_REVIEWER";

    private static final Set<String> STATUSES = Set.of(
            Task.CREATED, Task.IN_PROCESS, Task.REVIEW, Task.DONE);
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private static final int TITLE_LIMIT = 200;
    private static final int SHORT_LIMIT = 80;
    private static final int COMMENT_LIMIT = 20000;
    private static final int DESCRIPTION_LIMIT = 2_000_000;

    private final TaskRepository tasks;
    private final TaskFileRepository files;
    private final TaskAssigneeRepository assignees;
    private final TaskReviewerRepository reviewers;
    private final TaskGroupRepository taskGroups;
    private final AppUserRepository users;
    private final EmployeeGroupRepository employeeGroups;
    private final EmployeeGroupMemberRepository groupMembers;
    private final SubConfigTypeRepository configTypes;
    private final SubConfigEntryRepository configEntries;
    private final DocumentStorage storage;
    private final DocumentProperties documents;
    private final AppNotifier notifier;
    private final TaskLogRepository taskLogs;
    private final Clock clock;

    public TaskService(TaskRepository tasks,
                       TaskFileRepository files,
                       TaskAssigneeRepository assignees,
                       TaskReviewerRepository reviewers,
                       TaskGroupRepository taskGroups,
                       AppUserRepository users,
                       EmployeeGroupRepository employeeGroups,
                       EmployeeGroupMemberRepository groupMembers,
                       SubConfigTypeRepository configTypes,
                       SubConfigEntryRepository configEntries,
                       DocumentStorage storage,
                       DocumentProperties documents,
                       AppNotifier notifier,
                       TaskLogRepository taskLogs,
                       Clock clock) {
        this.tasks = tasks;
        this.files = files;
        this.assignees = assignees;
        this.reviewers = reviewers;
        this.taskGroups = taskGroups;
        this.users = users;
        this.employeeGroups = employeeGroups;
        this.groupMembers = groupMembers;
        this.configTypes = configTypes;
        this.configEntries = configEntries;
        this.storage = storage;
        this.documents = documents;
        this.notifier = notifier;
        this.taskLogs = taskLogs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list() {
        return respond(tasks.findAllByOrderByUpdatedAtDescTaskIdDesc());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listByQuote(Long qid) {
        if (qid == null) {
            return List.of();
        }
        return respond(tasks.findByQidOrderByUpdatedAtDescTaskIdDesc(qid));
    }

    private List<TaskResponse> respond(List<Task> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> taskIds = rows.stream().map(Task::getTaskId).toList();
        Map<Long, List<TaskAssignee>> assigneesByTask = new HashMap<>();
        Map<Long, List<TaskReviewer>> reviewersByTask = new HashMap<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (Task row : rows) {
            add(userIds, row.getCreatedBy());
        }
        for (TaskAssignee link : assignees.findByTaskIdIn(taskIds)) {
            assigneesByTask.computeIfAbsent(link.getTaskId(), key -> new ArrayList<>()).add(link);
            add(userIds, link.getUserId());
            add(userIds, link.getAssignedBy());
        }
        for (TaskReviewer link : reviewers.findByTaskIdIn(taskIds)) {
            reviewersByTask.computeIfAbsent(link.getTaskId(), key -> new ArrayList<>()).add(link);
            add(userIds, link.getUserId());
        }
        Map<Long, List<TaskGroupLink>> groupsByTask = new HashMap<>();
        Set<Long> groupIds = new LinkedHashSet<>();
        for (TaskGroupLink link : taskGroups.findByTaskIdIn(taskIds)) {
            groupsByTask.computeIfAbsent(link.getTaskId(), key -> new ArrayList<>()).add(link);
            groupIds.add(link.getGroupId());
        }
        Map<Long, List<TaskFileResponse>> filesByTask = new HashMap<>();
        for (TaskFile file : files.findByTaskIdInOrderByFileIdAsc(taskIds)) {
            filesByTask.computeIfAbsent(file.getTaskId(), key -> new ArrayList<>()).add(fileResponse(file));
        }
        Map<Long, String> names = names(userIds);
        Map<Long, String> groupNames = groupNames(groupIds);
        return rows.stream()
                .map(row -> toResponse(
                        row,
                        names,
                        assigneesByTask.getOrDefault(row.getTaskId(), List.of()),
                        reviewersByTask.getOrDefault(row.getTaskId(), List.of()),
                        groupsByTask.getOrDefault(row.getTaskId(), List.of()),
                        groupNames,
                        filesByTask.getOrDefault(row.getTaskId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskOptionsResponse options() {
        List<TaskPersonResponse> people = people();
        Set<Long> allowed = new LinkedHashSet<>();
        for (TaskPersonResponse person : people) {
            allowed.add(person.userId());
        }
        List<EmployeeGroup> groups = taskManagementGroups();
        Map<Long, List<Long>> members = new HashMap<>();
        if (!groups.isEmpty()) {
            List<Long> groupIds = groups.stream().map(EmployeeGroup::getGroupId).toList();
            for (EmployeeGroupMember member : groupMembers.findByGroupIdIn(groupIds)) {
                if (allowed.contains(member.getUserId())) {
                    members.computeIfAbsent(member.getGroupId(), key -> new ArrayList<>()).add(member.getUserId());
                }
            }
        }
        List<TaskGroupOption> groupOptions = groups.stream()
                .map(group -> new TaskGroupOption(
                        group.getGroupId(),
                        group.getName(),
                        members.getOrDefault(group.getGroupId(), List.of())))
                .toList();
        return new TaskOptionsResponse(taskTypes(), people, quoteReviewers(), groupOptions);
    }

    @Transactional(readOnly = true)
    public List<TaskPersonResponse> people() {
        return users.findByEnabledTrueOrderByDisplayNameAsc().stream()
                .map(user -> new TaskPersonResponse(user.getUserId(), display(user)))
                .toList();
    }

    public TaskResponse create(Long userId, TaskCreateRequest request) {
        if (request == null) {
            throw new TaskValidationException("Title is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Task task = new Task();
        task.setTitle(requireTitle(request.title()));
        task.setDescription(cleanText(request.description(), DESCRIPTION_LIMIT));
        task.setTaskType(cleanType(request.taskType(), null));
        task.setPriority(cleanPriority(request.priority()));
        task.setJobNumber(cleanShort(request.jobNumber(), "Job number"));
        task.setNcNumber(cleanShort(request.ncNumber(), "NC#"));
        task.setQid(request.qid());
        task.setReminderDate(cleanDate(request.reminderDate()));
        task.setStatus(Task.CREATED);
        task.setCreatedBy(userId);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task = tasks.save(task);
        replaceGroups(task.getTaskId(), request.groupIds());
        replaceAssignees(task, userId, request.assigneeIds() == null ? List.of() : request.assigneeIds(), true);
        replaceReviewers(task, userId, request.reviewerIds() == null ? List.of() : request.reviewerIds());
        logChange(task.getTaskId(), userId, "Status", null, statusText(task.getStatus()));
        logChange(task.getTaskId(), userId, "Job number", null, task.getJobNumber());
        logChange(task.getTaskId(), userId, "NC#", null, task.getNcNumber());
        logChange(task.getTaskId(), userId, "Reminder date", null, dateText(task.getReminderDate()));
        logChange(task.getTaskId(), userId, "Employee group", null, groupSummary(task.getTaskId()));
        return toResponse(task);
    }

    public TaskResponse update(Long taskId, Long userId, TaskUpdateRequest request) {
        if (request == null) {
            throw new TaskValidationException("Nothing to update");
        }
        Task task = requireTask(taskId);
        requireCreator(task, userId);
        if (request.title() != null) {
            task.setTitle(requireTitle(request.title()));
        }
        if (request.description() != null) {
            task.setDescription(cleanText(request.description(), DESCRIPTION_LIMIT));
        }
        if (request.taskType() != null) {
            task.setTaskType(cleanType(request.taskType(), task.getTaskType()));
        }
        if (request.priority() != null) {
            task.setPriority(cleanPriority(request.priority()));
        }
        if (request.jobNumber() != null) {
            String previous = task.getJobNumber();
            task.setJobNumber(cleanShort(request.jobNumber(), "Job number"));
            logChange(task.getTaskId(), userId, "Job number", previous, task.getJobNumber());
        }
        if (request.ncNumber() != null) {
            String previous = task.getNcNumber();
            task.setNcNumber(cleanShort(request.ncNumber(), "NC#"));
            logChange(task.getTaskId(), userId, "NC#", previous, task.getNcNumber());
        }
        if (request.reminderDate() != null) {
            String previous = dateText(task.getReminderDate());
            LocalDate next = cleanDate(request.reminderDate());
            task.setReminderDate(next);
            if (!Objects.equals(previous, dateText(next))) {
                task.setReminderNotifiedOn(null);
            }
            logChange(task.getTaskId(), userId, "Reminder date", previous, dateText(next));
        }
        if (request.groupsSpecified()) {
            String previous = groupSummary(task.getTaskId());
            replaceGroups(task.getTaskId(), request.groupIds());
            logChange(task.getTaskId(), userId, "Employee group", previous, groupSummary(task.getTaskId()));
        }
        if (request.assigneesSpecified()) {
            replaceAssignees(task, userId, request.assigneeIds() == null ? List.of() : request.assigneeIds(), false);
        }
        if (request.reviewerSpecified()) {
            String previous = reviewerSummary(task.getTaskId());
            replaceReviewers(task, userId, request.reviewerIds() == null ? List.of() : request.reviewerIds());
            logChange(task.getTaskId(), userId, "Reviewer", previous, reviewerSummary(task.getTaskId()));
        }
        syncAssigneeStatuses(task, userId);
        if (request.status() != null) {
            String next = requireStatus(request.status());
            String previous = task.getStatus();
            task.setStatus(next);
            logChange(task.getTaskId(), userId, "Status", statusText(previous), statusText(task.getStatus()));
        }
        if (request.commentSpecified() || request.personalStatus() != null) {
            applyPersonal(task, userId, request.personalStatus(), request.comment(), request.commentSpecified());
        }
        task.setUpdatedAt(LocalDateTime.now(clock));
        return toResponse(tasks.save(task));
    }

    public TaskResponse progress(Long taskId, Long userId, TaskProgressRequest request) {
        if (request == null) {
            throw new TaskValidationException("Nothing to update");
        }
        Task task = requireTask(taskId);
        if (reviewers.findByTaskIdAndUserId(taskId, userId).isPresent()
                && assignees.findByTaskIdAndUserId(taskId, userId).isEmpty()) {
            if (request.status() != null) {
                throw new TaskValidationException("A reviewer can comment on this task.");
            }
            if (request.commentSpecified()) {
                TaskReviewer row = reviewers.findByTaskIdAndUserId(taskId, userId).orElseThrow();
                String next = cleanText(request.comment(), COMMENT_LIMIT);
                if (!Objects.equals(row.getComment(), next)) {
                    row.setComment(next);
                    row.setCommentAt(next == null ? null : LocalDateTime.now(clock));
                    reviewers.save(row);
                }
            }
            task.setUpdatedAt(LocalDateTime.now(clock));
            return toResponse(tasks.save(task));
        }
        applyPersonal(task, userId, request.status(), request.comment(), request.commentSpecified());
        task.setUpdatedAt(LocalDateTime.now(clock));
        return toResponse(tasks.save(task));
    }

    public void sendDueReminders() {
        LocalDate today = LocalDate.now(clock);
        List<Task> due = tasks.findRemindersDue(today);
        if (due.isEmpty()) {
            return;
        }
        List<Long> taskIds = due.stream().map(Task::getTaskId).toList();
        Map<Long, List<Long>> people = new HashMap<>();
        for (TaskAssignee row : assignees.findByTaskIdIn(taskIds)) {
            people.computeIfAbsent(row.getTaskId(), key -> new ArrayList<>()).add(row.getUserId());
        }
        Map<Long, List<Long>> reviewerIds = new HashMap<>();
        for (TaskReviewer row : reviewers.findByTaskIdIn(taskIds)) {
            reviewerIds.computeIfAbsent(row.getTaskId(), key -> new ArrayList<>()).add(row.getUserId());
        }
        for (Task task : due) {
            if (task.getReminderDate() == null
                    || task.getReminderDate().isAfter(today)
                    || Task.DONE.equals(task.getStatus())
                    || task.getReminderDate().equals(task.getReminderNotifiedOn())) {
                continue;
            }
            List<Long> recipients = new ArrayList<>();
            addRecipient(recipients, task.getCreatedBy());
            for (Long reviewerId : reviewerIds.getOrDefault(task.getTaskId(), List.of())) {
                addRecipient(recipients, reviewerId);
            }
            for (Long userId : people.getOrDefault(task.getTaskId(), List.of())) {
                addRecipient(recipients, userId);
            }
            notifier.notifyUsers(
                    recipients,
                    "task-reminder",
                    "Task reminder",
                    task.getTitle() + " is due " + dateText(task.getReminderDate()) + ".",
                    "/tasks");
            task.setReminderNotifiedOn(task.getReminderDate());
            tasks.save(task);
        }
    }

    @Transactional(readOnly = true)
    public List<TaskLogResponse> logs(Long taskId) {
        requireTask(taskId);
        List<TaskLog> rows = taskLogs.findByTaskIdOrderByLogIdDesc(taskId);
        Set<Long> userIds = new LinkedHashSet<>();
        for (TaskLog row : rows) {
            add(userIds, row.getUserId());
        }
        Map<Long, String> userNames = names(userIds);
        List<TaskLogResponse> responses = new ArrayList<>();
        for (TaskLog row : rows) {
            responses.add(new TaskLogResponse(
                    row.getLogId(),
                    row.getFieldName(),
                    row.getOldValue(),
                    row.getNewValue(),
                    row.getUserId(),
                    name(userNames, row.getUserId()),
                    row.getCreatedAt()));
        }
        return responses;
    }

    public TaskFileResponse addFile(Long taskId, Long userId, MultipartFile file) {
        requireCreator(requireTask(taskId), userId);
        if (file == null || file.isEmpty()) {
            throw new TaskValidationException("Choose a file");
        }
        if (file.getSize() > documents.getMaxFileSize()) {
            throw new TaskValidationException("The file is larger than 10 MB.");
        }
        String original = displayName(file.getOriginalFilename());
        TaskFile row = new TaskFile();
        row.setTaskId(taskId);
        row.setOriginalFilename(original);
        row.setContentType(contentType(file));
        row.setFileSize(file.getSize());
        row.setStorageKey("pending");
        row.setCreatedAt(LocalDateTime.now(clock));
        row = files.saveAndFlush(row);
        String key = storageKey(taskId, row.getFileId(), original);
        try (InputStream input = file.getInputStream()) {
            DocumentStorage.StoredDocument stored = storage.store(key, input);
            row.setStorageKey(key);
            row.setFileSize(stored.size());
            row = files.save(row);
            touch(taskId);
            return fileResponse(row);
        } catch (IOException | RuntimeException exception) {
            files.delete(row);
            try {
                storage.delete(key);
            } catch (IOException cleanup) {
                exception.addSuppressed(cleanup);
            }
            if (exception instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unable to store the file", exception);
        }
    }

    public void deleteFile(Long taskId, Long userId, Long fileId) {
        Task task = requireTask(taskId);
        requireCreator(task, userId);
        if (!stillCreated(task)) {
            throw new TaskValidationException("Files can only be removed while the task is Created.");
        }
        TaskFile file = openFile(taskId, fileId);
        try {
            storage.delete(file.getStorageKey());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete the file", exception);
        }
        files.delete(file);
        touch(taskId);
    }

    @Transactional(readOnly = true)
    public TaskFile openFile(Long taskId, Long fileId) {
        TaskFile file = files.findById(fileId).orElseThrow(() -> TaskNotFoundException.file(fileId));
        if (!taskId.equals(file.getTaskId())) {
            throw TaskNotFoundException.file(fileId);
        }
        return file;
    }

    private void replaceGroups(Long taskId, List<Long> groupIds) {
        List<Long> ids = distinct(groupIds);
        Set<Long> allowed = new LinkedHashSet<>();
        for (EmployeeGroup group : taskManagementGroups()) {
            allowed.add(group.getGroupId());
        }
        for (Long id : ids) {
            if (!allowed.contains(id)) {
                throw new TaskValidationException("Choose an employee group from the list");
            }
        }
        taskGroups.deleteByTaskId(taskId);
        taskGroups.flush();
        for (Long id : ids) {
            TaskGroupLink link = new TaskGroupLink();
            link.setTaskId(taskId);
            link.setGroupId(id);
            taskGroups.save(link);
        }
    }

    private void replaceAssignees(Task task, Long actorId, List<Long> userIds, boolean creating) {
        List<Long> ids = distinct(userIds);
        Set<Long> allowed = assignableIds();
        boolean development = isDevelopment(task.getTaskType());
        Map<Long, TaskAssignee> existing = new HashMap<>();
        for (TaskAssignee row : assignees.findByTaskIdOrderByUserIdAsc(task.getTaskId())) {
            existing.put(row.getUserId(), row);
            allowed.add(row.getUserId());
        }
        for (Long id : ids) {
            if (!allowed.contains(id)) {
                throw new TaskValidationException("Choose a person from the list");
            }
        }
        if (ids.isEmpty()) {
            if (!existing.isEmpty()) {
                assignees.deleteByTaskId(task.getTaskId());
            }
            return;
        }
        assignees.deleteByTaskIdAndUserIdNotIn(task.getTaskId(), ids);
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long id : ids) {
            TaskAssignee row = existing.get(id);
            if (row == null) {
                row = new TaskAssignee();
                row.setTaskId(task.getTaskId());
                row.setUserId(id);
                row.setAssignedBy(actorId);
                row.setStatus(development ? null : Task.CREATED);
                row.setUpdatedAt(now);
                assignees.save(row);
                if (!id.equals(actorId)) {
                    notifyAssignee(actorId, id);
                }
            } else if (development) {
                String previous = row.getStatus();
                row.setStatus(null);
                assignees.save(row);
                logChange(task.getTaskId(), actorId, "Status", statusText(previous), null);
            } else if (row.getStatus() == null) {
                String kept = keptStatus(task);
                row.setStatus(kept);
                row.setUpdatedAt(now);
                assignees.save(row);
                logChange(task.getTaskId(), actorId, "Status", null, statusText(kept));
            } else if (creating) {
                assignees.save(row);
            }
        }
    }

    private void syncAssigneeStatuses(Task task, Long userId) {
        boolean development = isDevelopment(task.getTaskType());
        LocalDateTime now = LocalDateTime.now(clock);
        for (TaskAssignee row : assignees.findByTaskIdOrderByUserIdAsc(task.getTaskId())) {
            if (development && row.getStatus() != null) {
                String previous = row.getStatus();
                row.setStatus(null);
                row.setUpdatedAt(now);
                assignees.save(row);
                logChange(task.getTaskId(), userId, "Status", statusText(previous), null);
            } else if (!development && (row.getStatus() == null || row.getStatus().isBlank())) {
                String kept = keptStatus(task);
                row.setStatus(kept);
                row.setUpdatedAt(now);
                assignees.save(row);
                logChange(task.getTaskId(), userId, "Status", null, statusText(kept));
            }
        }
    }

    private void applyPersonal(Task task, Long userId, String status, String comment, boolean commentSpecified) {
        TaskAssignee row = assignees.findByTaskIdAndUserId(task.getTaskId(), userId)
                .orElseThrow(() -> new TaskValidationException("This task is not assigned to you"));
        String previousStatus = row.getStatus();
        if (status != null) {
            if (isDevelopment(task.getTaskType())) {
                throw new TaskValidationException("This task uses the creator's status. Add a comment instead.");
            }
            row.setStatus(requireStatus(status));
        }
        if (commentSpecified) {
            String next = cleanText(comment, COMMENT_LIMIT);
            if (!Objects.equals(row.getComment(), next)) {
                row.setComment(next);
                row.setCommentAt(next == null ? null : LocalDateTime.now(clock));
            }
        }
        if (status != null) {
            logChange(task.getTaskId(), userId, "Status", statusText(previousStatus), statusText(row.getStatus()));
        }
        row.setUpdatedAt(LocalDateTime.now(clock));
        assignees.save(row);
    }

    private void touch(Long taskId) {
        tasks.findById(taskId).ifPresent(task -> {
            task.setUpdatedAt(LocalDateTime.now(clock));
            tasks.save(task);
        });
    }

    private void replaceReviewers(Task task, Long actorId, List<Long> userIds) {
        List<Long> ids = distinct(userIds);
        Map<Long, TaskReviewer> existing = new HashMap<>();
        for (TaskReviewer row : reviewers.findByTaskIdOrderByUserIdAsc(task.getTaskId())) {
            existing.put(row.getUserId(), row);
        }
        if (ids.isEmpty()) {
            if (!existing.isEmpty()) {
                reviewers.deleteByTaskId(task.getTaskId());
            }
            return;
        }
        Set<Long> allowed = quoteReviewerIds();
        existing.keySet().forEach(allowed::add);
        for (Long id : ids) {
            if (!allowed.contains(id)) {
                throw new TaskValidationException("Choose a reviewer from the list");
            }
        }
        reviewers.deleteByTaskIdAndUserIdNotIn(task.getTaskId(), ids);
        List<Long> added = new ArrayList<>();
        for (Long id : ids) {
            if (existing.containsKey(id)) {
                continue;
            }
            TaskReviewer row = new TaskReviewer();
            row.setTaskId(task.getTaskId());
            row.setUserId(id);
            reviewers.save(row);
            added.add(id);
        }
        notifyReviewers(actorId, added);
    }

    private List<TaskPersonResponse> quoteReviewers() {
        return users.findEnabledByRoleCode(QUOTE_REVIEWER).stream()
                .map(user -> new TaskPersonResponse(user.getUserId(), display(user)))
                .toList();
    }

    private Set<Long> quoteReviewerIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (AppUser user : users.findEnabledByRoleCode(QUOTE_REVIEWER)) {
            ids.add(user.getUserId());
        }
        return ids;
    }

    private String reviewerSummary(Long taskId) {
        List<String> names = new ArrayList<>();
        for (TaskReviewer row : reviewers.findByTaskIdOrderByUserIdAsc(taskId)) {
            String name = personName(row.getUserId());
            if (name != null) {
                names.add(name);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private void notifyReviewers(Long actorId, List<Long> reviewerIds) {
        List<Long> recipients = new ArrayList<>();
        for (Long reviewerId : reviewerIds) {
            if (reviewerId != null && !reviewerId.equals(actorId)) {
                recipients.add(reviewerId);
            }
        }
        if (recipients.isEmpty()) {
            return;
        }
        String name = users.findById(actorId).map(this::display).orElse("Someone");
        notifier.notifyUsers(
                recipients,
                "task-review",
                "Task review",
                name + " asked you to review a task.",
                "/tasks");
    }

    private String personName(Long userId) {
        if (userId == null) {
            return null;
        }
        return users.findById(userId).map(this::display).orElse("Unknown");
    }

    private void notifyAssignee(Long actorId, Long assigneeId) {
        String name = users.findById(actorId).map(this::display).orElse("Someone");
        notifier.notifyUsers(
                List.of(assigneeId),
                "Task assigned",
                "Task assigned",
                name + " assigned this task to you.",
                "/tasks");
    }

    private Task requireTask(Long taskId) {
        return tasks.findById(taskId).orElseThrow(() -> TaskNotFoundException.task(taskId));
    }

    private boolean stillCreated(Task task) {
        if (!Task.CREATED.equals(task.getStatus())) {
            return false;
        }
        for (TaskAssignee row : assignees.findByTaskIdOrderByUserIdAsc(task.getTaskId())) {
            String status = row.getStatus();
            if (status != null && !status.isBlank() && !Task.CREATED.equals(status)) {
                return false;
            }
        }
        return true;
    }

    private void requireCreator(Task task, Long userId) {
        if (!userId.equals(task.getCreatedBy())) {
            throw new TaskValidationException("Only the person who created this task can change it.");
        }
    }

    private Set<Long> assignableIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (AppUser user : users.findByEnabledTrueOrderByDisplayNameAsc()) {
            ids.add(user.getUserId());
        }
        return ids;
    }

    private List<String> taskTypes() {
        return configTypes.findAllByOrderByTypeNameAsc().stream()
                .filter(type -> TASK_TYPES_NAME.equalsIgnoreCase(type.getTypeName() == null ? "" : type.getTypeName().trim()))
                .findFirst()
                .map(type -> configEntries.findByTypeTypeIdOrderByCreatedAtDesc(type.getTypeId()).stream()
                        .map(entry -> entry.getFieldValue(1))
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList())
                .orElse(List.of());
    }

    private List<EmployeeGroup> taskManagementGroups() {
        return employeeGroups.findByDeletedFalseAndTypeIgnoreCaseOrderByNameAsc(TASK_MANAGEMENT_GROUP);
    }

    private TaskResponse toResponse(Task task) {
        List<TaskAssignee> people = assignees.findByTaskIdOrderByUserIdAsc(task.getTaskId());
        List<TaskReviewer> reviewerRows = reviewers.findByTaskIdOrderByUserIdAsc(task.getTaskId());
        List<TaskGroupLink> links = taskGroups.findByTaskId(task.getTaskId());
        Set<Long> userIds = new LinkedHashSet<>();
        add(userIds, task.getCreatedBy());
        for (TaskReviewer reviewer : reviewerRows) {
            add(userIds, reviewer.getUserId());
        }
        for (TaskAssignee person : people) {
            add(userIds, person.getUserId());
            add(userIds, person.getAssignedBy());
        }
        Set<Long> groupIds = new LinkedHashSet<>();
        for (TaskGroupLink link : links) {
            groupIds.add(link.getGroupId());
        }
        List<TaskFileResponse> attached = files.findByTaskIdOrderByFileIdAsc(task.getTaskId()).stream()
                .map(TaskService::fileResponse)
                .toList();
        return toResponse(task, names(userIds), people, reviewerRows, links, groupNames(groupIds), attached);
    }

    private TaskResponse toResponse(Task task,
                                    Map<Long, String> names,
                                    List<TaskAssignee> people,
                                    List<TaskReviewer> reviewerRows,
                                    List<TaskGroupLink> links,
                                    Map<Long, String> groupNames,
                                    List<TaskFileResponse> attached) {
        List<TaskAssigneeResponse> assigneeResponses = new ArrayList<>();
        for (TaskAssignee person : people) {
            assigneeResponses.add(new TaskAssigneeResponse(
                    person.getUserId(),
                    name(names, person.getUserId()),
                    person.getStatus(),
                    person.getComment(),
                    person.getCommentAt(),
                    person.getAssignedBy(),
                    name(names, person.getAssignedBy())));
        }
        assigneeResponses.sort((left, right) -> String.valueOf(left.displayName()).compareToIgnoreCase(String.valueOf(right.displayName())));
        List<TaskReviewerResponse> reviewerResponses = new ArrayList<>();
        for (TaskReviewer reviewer : reviewerRows) {
            reviewerResponses.add(new TaskReviewerResponse(
                    reviewer.getUserId(),
                    name(names, reviewer.getUserId()),
                    reviewer.getComment(),
                    reviewer.getCommentAt()));
        }
        reviewerResponses.sort((left, right) -> String.valueOf(left.displayName()).compareToIgnoreCase(String.valueOf(right.displayName())));
        List<TaskGroupResponse> groupResponses = new ArrayList<>();
        for (TaskGroupLink link : links) {
            groupResponses.add(new TaskGroupResponse(link.getGroupId(), groupNames.getOrDefault(link.getGroupId(), "Unknown")));
        }
        groupResponses.sort((left, right) -> left.name().compareToIgnoreCase(right.name()));
        return new TaskResponse(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskType(),
                task.getPriority(),
                task.getJobNumber(),
                task.getNcNumber(),
                task.getQid(),
                task.getReminderDate(),
                task.getStatus(),
                task.getCreatedBy(),
                name(names, task.getCreatedBy()),
                reviewerResponses,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                assigneeResponses,
                groupResponses,
                attached);
    }

    private void logChange(Long taskId, Long userId, String field, String oldValue, String newValue) {
        String previous = clip(oldValue);
        String next = clip(newValue);
        if (Objects.equals(previous, next)) {
            return;
        }
        TaskLog row = new TaskLog();
        row.setTaskId(taskId);
        row.setUserId(userId);
        row.setFieldName(field);
        row.setOldValue(previous);
        row.setNewValue(next);
        row.setCreatedAt(LocalDateTime.now(clock));
        taskLogs.save(row);
    }

    private String groupSummary(Long taskId) {
        List<TaskGroupLink> links = taskGroups.findByTaskId(taskId);
        if (links.isEmpty()) {
            return null;
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (TaskGroupLink link : links) {
            ids.add(link.getGroupId());
        }
        Map<Long, String> labels = groupNames(ids);
        List<String> names = new ArrayList<>();
        for (Long id : ids) {
            names.add(labels.getOrDefault(id, "Unknown"));
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(", ", names);
    }

    private static void addRecipient(List<Long> recipients, Long userId) {
        if (userId != null && !recipients.contains(userId)) {
            recipients.add(userId);
        }
    }

    private static final DateTimeFormatter REMINDER_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private static String dateText(LocalDate date) {
        return date == null ? null : date.format(REMINDER_DATE);
    }

    private static LocalDate cleanDate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException exception) {
            throw new TaskValidationException("Reminder date is not a valid date");
        }
    }

    private static String statusText(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status) {
            case Task.CREATED -> "Created";
            case Task.IN_PROCESS -> "In Process";
            case Task.REVIEW -> "Review";
            case Task.DONE -> "Done";
            default -> status;
        };
    }

    private static String clip(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private Map<Long, String> groupNames(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        employeeGroups.findAllById(groupIds).forEach(group -> names.put(group.getGroupId(), group.getName()));
        return names;
    }

    private Map<Long, String> names(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        users.findAllById(userIds).forEach(user -> names.put(user.getUserId(), display(user)));
        return names;
    }

    private String display(AppUser user) {
        String name = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        if (!name.isEmpty()) {
            return name;
        }
        return user.getEmail() == null ? "Unknown" : user.getEmail();
    }

    private static String name(Map<Long, String> names, Long userId) {
        if (userId == null) {
            return null;
        }
        return names.getOrDefault(userId, "Unknown");
    }

    private static void add(Set<Long> ids, Long userId) {
        if (userId != null) {
            ids.add(userId);
        }
    }

    private static List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(id -> id != null).distinct().toList();
    }

    static boolean isDevelopment(String taskType) {
        return taskType != null && DEVELOPMENT.equalsIgnoreCase(taskType.trim());
    }

    private static String keptStatus(Task task) {
        String status = task.getStatus();
        return status == null || status.isBlank() ? Task.CREATED : status;
    }

    private String cleanType(String requested, String current) {
        if (requested == null) {
            return current;
        }
        if (requested.isBlank()) {
            return null;
        }
        String value = requested.trim();
        if (current != null && current.equalsIgnoreCase(value)) {
            return current;
        }
        List<String> allowed = taskTypes();
        if (allowed.isEmpty()) {
            throw new TaskValidationException("Add Task types in Masters Data first.");
        }
        return allowed.stream()
                .filter(item -> item.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new TaskValidationException("Choose a task type from the list"));
    }

    private static String cleanPriority(String priority) {
        if (priority == null) {
            return null;
        }
        if (priority.isBlank()) {
            return null;
        }
        String value = priority.trim().toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(value)) {
            throw new TaskValidationException("Priority must be Low, Medium, or High");
        }
        return value;
    }

    private static String cleanShort(String value, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > SHORT_LIMIT) {
            throw new TaskValidationException(label + " must be 80 characters or less");
        }
        return trimmed;
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("Title is required");
        }
        String value = title.trim();
        if (value.length() > TITLE_LIMIT) {
            throw new TaskValidationException("Title must be 200 characters or less");
        }
        return value;
    }

    private static String cleanText(String description, int limit) {
        if (description == null) {
            return null;
        }
        String value = description.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > limit) {
            throw new TaskValidationException(limit == DESCRIPTION_LIMIT ? "Description is too long" : "Comment is too long");
        }
        return value;
    }

    private static String requireStatus(String status) {
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(value)) {
            throw new TaskValidationException("Status must be Created, In Process, Review, or Done");
        }
        return value;
    }

    private static String contentType(MultipartFile file) {
        String type = file.getContentType();
        return type == null || type.isBlank() ? "application/octet-stream" : type;
    }

    private static TaskFileResponse fileResponse(TaskFile file) {
        return new TaskFileResponse(file.getFileId(), file.getOriginalFilename(), file.getFileSize());
    }

    static String displayName(String filename) {
        String value = filename == null ? "file" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).trim();
        if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
            return "file";
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    static String storageKey(Long taskId, Long fileId, String originalFilename) {
        String safe = displayName(originalFilename).replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank() || safe.chars().allMatch(ch -> ch == '_' || ch == '.')) {
            safe = "file";
        }
        String prefix = "task/" + taskId + "/" + fileId + "-";
        int room = 255 - prefix.length();
        if (room < 1) {
            room = 1;
        }
        if (safe.length() > room) {
            safe = safe.substring(0, room);
        }
        return prefix + safe;
    }
}
