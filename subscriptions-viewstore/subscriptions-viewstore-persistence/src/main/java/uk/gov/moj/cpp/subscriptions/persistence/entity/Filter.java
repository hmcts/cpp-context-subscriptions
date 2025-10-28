package uk.gov.moj.cpp.subscriptions.persistence.entity;

import uk.gov.moj.cpp.subscriptions.persistence.constants.FilterType;
import uk.gov.moj.cpp.subscriptions.persistence.constants.Gender;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "filter")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class Filter implements Serializable {

    private static final long serialVersionUID = 6164363256076905563L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "urn")
    private String urn;


    @Column(name = "is_adult")
    private Boolean adult;

    @Column(name = "defendant_first_name")
    private String defendantFirstName;

    @Column(name = "defendant_last_name")
    private String defendantLastName;


    @Column(name = "defendant_date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "offence")
    private String offence;

    @Column(name = "filter_type")
    @Enumerated(EnumType.STRING)
    private FilterType filterType;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }

    public Boolean isAdult() {
        return adult;
    }

    public void setAdult(final Boolean adult) {
        this.adult = adult;
    }

    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public void setDefendantFirstName(final String defendantFirstName) {
        this.defendantFirstName = defendantFirstName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public void setDefendantLastName(final String defendantLastName) {
        this.defendantLastName = defendantLastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public String getOffence() {
        return offence;
    }

    public void setOffence(final String offence) {
        this.offence = offence;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(final FilterType filterType) {
        this.filterType = filterType;
    }


    public static FiltersBuilder builder() {
        return new FiltersBuilder();
    }

    public static final class FiltersBuilder {
        private UUID id;
        private String urn;
        private Boolean adult;
        private String defendantFirstName;
        private String defendantLastName;
        private LocalDate dateOfBirth;
        private Gender gender;
        private String offence;
        private FilterType filterType;

        private FiltersBuilder() {
        }

        public FiltersBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public FiltersBuilder withUrn(final String urn) {
            this.urn = urn;
            return this;
        }

        public FiltersBuilder withAdult(final Boolean adult) {
            this.adult = adult;
            return this;
        }

        public FiltersBuilder withDefendantFirstName(final String defendantFirstName) {
            this.defendantFirstName = defendantFirstName;
            return this;
        }

        public FiltersBuilder withDefendantLastName(final String defendantLastName) {
            this.defendantLastName = defendantLastName;
            return this;
        }

        public FiltersBuilder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public FiltersBuilder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public FiltersBuilder withOffence(final String offence) {
            this.offence = offence;
            return this;
        }

        public FiltersBuilder withFilterType(final FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        public Filter build() {
            final Filter filter = new Filter();
            filter.setId(id);
            filter.setUrn(urn);
            filter.setAdult(adult);
            filter.setDefendantFirstName(defendantFirstName);
            filter.setDefendantLastName(defendantLastName);
            filter.setDateOfBirth(dateOfBirth);
            filter.setGender(gender);
            filter.setOffence(offence);
            filter.setFilterType(filterType);
            return filter;
        }
    }
}
