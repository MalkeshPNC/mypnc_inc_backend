package com.pnc.masters.customer.reminder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderRulesTest {

    @Test
    void onlyARunningBatchCanClaimAPendingRow() {
        assertThat(ReminderRules.claimable(ReminderBatch.RUNNING, ReminderRecipient.PENDING)).isTrue();
        assertThat(ReminderRules.claimable(ReminderBatch.PAUSED, ReminderRecipient.PENDING)).isFalse();
        assertThat(ReminderRules.claimable(ReminderBatch.STOPPED, ReminderRecipient.PENDING)).isFalse();
        assertThat(ReminderRules.claimable(ReminderBatch.RUNNING, ReminderRecipient.SENT)).isFalse();
        assertThat(ReminderRules.claimable(ReminderBatch.RUNNING, ReminderRecipient.FAILED)).isFalse();
        assertThat(ReminderRules.claimable(ReminderBatch.RUNNING, ReminderRecipient.SENDING)).isFalse();
        assertThat(ReminderRules.claimable(ReminderBatch.RUNNING, ReminderRecipient.NOT_CONFIRMED)).isFalse();
    }

    @Test
    void aRestartDoesNotQueueARowThatMayAlreadyHaveBeenAccepted() {
        assertThat(ReminderRules.afterRestart(ReminderRecipient.SENDING)).isEqualTo(ReminderRecipient.NOT_CONFIRMED);
        assertThat(ReminderRules.afterRestart(ReminderRecipient.PENDING)).isEqualTo(ReminderRecipient.PENDING);
        assertThat(ReminderRules.afterRestart(ReminderRecipient.SENT)).isEqualTo(ReminderRecipient.SENT);
        assertThat(ReminderRules.afterRestart(ReminderRecipient.FAILED)).isEqualTo(ReminderRecipient.FAILED);
    }

    @Test
    void sendTheRestQueuesFailedRowsAndLeavesSentAlone() {
        assertThat(ReminderRules.requeueForRest(ReminderRecipient.FAILED)).isTrue();
        assertThat(ReminderRules.requeueForRest(ReminderRecipient.PENDING)).isFalse();
        assertThat(ReminderRules.requeueForRest(ReminderRecipient.SENT)).isFalse();
        assertThat(ReminderRules.requeueForRest(ReminderRecipient.NOT_CONFIRMED)).isFalse();
        assertThat(ReminderRules.requeueForAgain(ReminderRecipient.SENT)).isTrue();
        assertThat(ReminderRules.requeueForAgain(ReminderRecipient.FAILED)).isFalse();
    }
}
