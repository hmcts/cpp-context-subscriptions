package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing;

import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.hearing.courts.HearingResulted.hearingResulted;
import static uk.gov.moj.cpp.subscriptions.json.schemas.Subscription.subscription;

import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.moj.cpp.subscriptions.event.processor.helper.FileResourceObjectMapper;
import uk.gov.moj.cpp.subscriptions.event.processor.service.ApplicationParameters;
import uk.gov.moj.cpp.subscriptions.event.processor.service.HearingService;
import uk.gov.moj.cpp.subscriptions.json.schemas.EmailInfo;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationEventRuleExecutorTest {

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private HearingService hearingService;


    @InjectMocks
    private NotificationEventRuleExecutor notificationEventRuleExecutor;

    private FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();


    @Test
    public void shouldExecuteReturnEmailList() throws IOException {
        final HearingResulted hearingResulted = fileResourceObjectMapper.convertFromFile("stub/NotificationEventRuleExecuterHearingResulted.json", HearingResulted.class);
        final Subscription subscription = fileResourceObjectMapper.convertFromFile("stub/CreateSubscriptionForSpecificSubscriberCommand.json", Subscription.class);

        final List<Subscription> subscriptions = singletonList(subscription);

        List<EmailInfo> emailInfos = notificationEventRuleExecutor.execute(hearingResulted.getHearing(), subscriptions);
        assertThat(emailInfos, hasSize(1));

        assertThat(emailInfos.get(0).getSubject(), is("Case URN123 - plea entered"));


        assertThat(emailInfos.get(0).getBody(), is(format("Robert ORMSBY - 17 January 1968. Occupy reserved seat / berth without a valid ticket on the Tyne and Wear Metro. Plea: GUILTY. ")));

    }

    @Test
    public void shouldReturnEmptyEmailListWhenProsecutionCasesIsNull() {
        final UUID courtId = randomUUID();
        final HearingResulted hearingResulted = hearingResulted().withHearing(hearing().withCourtCentre(courtCentre().withId(courtId).build()).build()).build();
        final List<Subscription> subscriptions = singletonList(subscription().build());

        List<EmailInfo> emailInfos = notificationEventRuleExecutor.execute(hearingResulted.getHearing(), subscriptions);

        assertThat(emailInfos.size(), is(0));
    }

    @Test
    public void shouldReturnEmptyEmailListWhenHearingIsNull() {

        final HearingResulted hearingResulted = hearingResulted().build();
        final List<Subscription> subscriptions = singletonList(subscription().build());

        List<EmailInfo> emailInfos = notificationEventRuleExecutor.execute(hearingResulted.getHearing(), subscriptions);

        assertThat(emailInfos.size(), is(0));
    }
}
