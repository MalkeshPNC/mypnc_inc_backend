package com.pnc.masters.quote.mail;

import com.pnc.masters.quote.api.QuoteValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteEmailAddressesTest {

    @Test
    void splitsCommaAndSemicolonLists() {
        assertThat(QuoteEmailAddresses.parse("a@example.com; b@example.com, c@example.com"))
                .containsExactly("a@example.com", "b@example.com", "c@example.com");
    }

    @Test
    void rejectsAnInvalidAddress() {
        assertThatThrownBy(() -> QuoteEmailAddresses.parse("not-an-email"))
                .isInstanceOf(QuoteValidationException.class);
    }

    @Test
    void requiresAToAddressAndCapsTheTotal() {
        assertThatThrownBy(() -> QuoteEmailAddresses.requireLimits(List.of(), List.of()))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessage("Enter at least one To address.");

        List<String> ten = List.of("1@e.com", "2@e.com", "3@e.com", "4@e.com", "5@e.com",
                "6@e.com", "7@e.com", "8@e.com", "9@e.com", "10@e.com");
        assertThatThrownBy(() -> QuoteEmailAddresses.requireLimits(ten, List.of("11@e.com")))
                .isInstanceOf(QuoteValidationException.class)
                .hasMessage("A quote email can include at most 10 addresses.");
    }
}
