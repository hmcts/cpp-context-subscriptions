package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static uk.gov.justice.hearing.courts.AttendanceType.BY_VIDEO;
import static uk.gov.justice.hearing.courts.AttendanceType.IN_PERSON;
import static uk.gov.justice.hearing.courts.AttendanceType.NOT_PRESENT;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events.Section.buildSection;
import static uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy.createFilter;

import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.AttendanceType;
import uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.filters.AbstractFilterStrategy;
import uk.gov.moj.cpp.subscriptions.json.schemas.Subscription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;


public class DefendantPresentEventRule extends HearingEventRule {

    private Hearing hearing;
    private ProsecutionCase prosecutionCase;
    private Subscription subscription;
    private AbstractFilterStrategy filterStrategy;
    private Predicate<DefendantAttendance> defendantPresentFilter = d -> d.getAttendanceDays().stream()
            .anyMatch(s -> asList(IN_PERSON, BY_VIDEO, NOT_PRESENT).contains(s.getAttendanceType()));


    public DefendantPresentEventRule(final Hearing hearing, final ProsecutionCase prosecutionCase, final Subscription subscription) {
        this.hearing = hearing;
        this.prosecutionCase = prosecutionCase;
        this.subscription = subscription;
        this.filterStrategy = createFilter(subscription);
    }

    private boolean defendantPresent() {
        return nonNull(hearing.getDefendantAttendance()) && hearing.getDefendantAttendance()
                .stream()
                .anyMatch(defendantPresentFilter);
    }

    private boolean containsCaseUrn() {
        return nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
    }

    @Override
    public boolean shouldExecute() {
        return defendantPresent() && containsCaseUrn() && filterStrategy.caseMatches(prosecutionCase);
    }

    @Override
    protected UUID getCaseId() {
        return prosecutionCase.getId();
    }

    @Override
    protected String getCaseUrn() {
        return prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
    }

    @Override
    protected String getTitle() {
        return "Defendant present";
    }

    @Override
    protected Subscription getSubscription() {
        return subscription;
    }

    protected List<Section> getSections() {
        return filterStrategy.filterDefendants(prosecutionCase)
                .stream()
                .filter(hearingHasAttendance())
                .map(this::prepareDefendantsInfo)
                .collect(toList());
    }

    private Predicate<Defendant> hearingHasAttendance() {
        final List<UUID> defendantsInHearing = hearing.getDefendantAttendance()
                .stream()
                .filter(defendantPresentFilter)
                .map(DefendantAttendance::getDefendantId)
                .collect(toList());

        return d -> defendantsInHearing.contains(d.getId());
    }

    private Section prepareDefendantsInfo(Defendant defendant) {
        final List<String> parameters = new ArrayList<>();
        parameters.add(prepareDefendantLine(defendant));
        parameters.addAll(getDefendantAttendanceType(defendant));

        return buildSection(parameters
                .stream()
                .toArray(String[]::new));
    }

    private List<String> getDefendantAttendanceType(Defendant defendant) {

        final Comparator<AttendanceDay> nameComparator = (a1, a2) -> {
            int result = getAttendanceTypeOrder(a1.getAttendanceType()).compareTo(getAttendanceTypeOrder(a2.getAttendanceType()));
            if (result != 0) {
                return result;
            }

            return a2.getDay().compareTo(a1.getDay());
        };

        return hearing.getDefendantAttendance().stream()
                .filter(a -> a.getDefendantId().equals(defendant.getId()))
                .flatMap(a -> a.getAttendanceDays()
                        .stream()
                        .sorted(nameComparator)
                        .map(b -> format("{0} - {1}", capitalize(b.getAttendanceType().toString().replace("_", " ").toLowerCase()), formatDateOfBirth(b.getDay()))))
                .collect(toList());
    }

    private Integer getAttendanceTypeOrder(AttendanceType attendanceType) {
        switch (attendanceType) {
            case IN_PERSON:
                return 0;
            case BY_VIDEO:
                return 1;
            default:
                return 2;
        }
    }


}
