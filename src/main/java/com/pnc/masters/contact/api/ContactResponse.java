package com.pnc.masters.contact.api;

public record ContactResponse(
        Long contId,
        Long customerId,
        String firstName,
        String lastName,
        String phone,
        String email,
        String contactPerson
) {
}