package com.pnc.masters.customer.reminder;

import com.pnc.masters.customer.reminder.ReminderRequests.ReminderBatchRequest;
import com.pnc.masters.customer.reminder.ReminderRequests.ReminderReviseRequest;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderActiveResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderAudienceResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderBatchResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderLogDetailResponse;
import com.pnc.masters.customer.reminder.ReminderResponses.ReminderLogPageResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/customers/reminders")
public class ReminderController {

    private final ReminderAudienceService audience;
    private final ReminderEmailService emails;

    public ReminderController(ReminderAudienceService audience, ReminderEmailService emails) {
        this.audience = audience;
        this.emails = emails;
    }

    @GetMapping("/audience")
    public ReminderAudienceResponse audience(@RequestParam String kind) {
        return audience.audience(kind);
    }

    @GetMapping("/active")
    public ReminderActiveResponse active(@RequestParam String kind) {
        return emails.active(kind);
    }

    @GetMapping("/log")
    public ReminderLogPageResponse log(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return emails.log(kind, status, page, size);
    }

    @GetMapping("/log/{recipientId}")
    public ReminderLogDetailResponse logDetail(@PathVariable Long recipientId) {
        return emails.logDetail(recipientId);
    }

    @GetMapping("/batches/{id}")
    public ReminderBatchResponse get(@PathVariable Long id) {
        return emails.get(id);
    }

    @PostMapping(path = "/batches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReminderBatchResponse create(@RequestPart("request") ReminderBatchRequest request,
                                        @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                        Authentication authentication) {
        return emails.create(userId(authentication), request, files);
    }

    @PostMapping("/batches/{id}/pause")
    public ReminderBatchResponse pause(@PathVariable Long id) {
        return emails.pause(id);
    }

    @PostMapping("/batches/{id}/resume")
    public ReminderBatchResponse resume(@PathVariable Long id) {
        return emails.resume(id);
    }

    @PostMapping("/batches/{id}/stop")
    public ReminderBatchResponse stop(@PathVariable Long id) {
        return emails.stop(id);
    }

    @PostMapping(path = "/batches/{id}/send-rest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReminderBatchResponse sendRest(@PathVariable Long id,
                                          @RequestPart("request") ReminderReviseRequest request,
                                          @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return emails.sendRest(id, request, files);
    }

    @PostMapping(path = "/batches/{id}/send-again", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReminderBatchResponse sendAgain(@PathVariable Long id,
                                           @RequestPart("request") ReminderReviseRequest request,
                                           @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return emails.sendAgain(id, request, files);
    }

    @PostMapping("/batches/{id}/recipients/{recipientId}/mark-sent")
    public ReminderBatchResponse markSent(@PathVariable Long id, @PathVariable Long recipientId) {
        return emails.markSent(id, recipientId);
    }

    @PostMapping("/batches/{id}/recipients/{recipientId}/retry")
    public ReminderBatchResponse retry(@PathVariable Long id, @PathVariable Long recipientId) {
        return emails.retry(id, recipientId);
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
