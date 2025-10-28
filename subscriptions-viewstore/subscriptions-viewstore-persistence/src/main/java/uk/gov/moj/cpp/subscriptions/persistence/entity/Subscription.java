package uk.gov.moj.cpp.subscriptions.persistence.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name = "subscription")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class Subscription implements Serializable {

    private static final long serialVersionUID = 5469813744527379259L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "active")
    private boolean active;

    @Column(name ="organisation_id")
    private UUID organisationId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "subscription", orphanRemoval = true)
    private Set<Subscriber> subscribers = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "filter_id")
    private Filter filter;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "subscription", orphanRemoval = true)
    private Set<Court> courts = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "subscription", orphanRemoval = true)
    private Set<NowsEdt> nowsEdts = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "subscription", orphanRemoval = true)
    private Set<Event> events = new HashSet<>();


    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(final UUID organisationId) {
        this.organisationId = organisationId;
    }

    public Set<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(final Set<Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(final Filter filter) {
        this.filter = filter;
    }

    public Set<Court> getCourts() {
        return courts;
    }

    public void setCourts(final Set<Court> courts) {
        this.courts = courts;
    }

    public Set<NowsEdt> getNowsEdts() {
        return nowsEdts;
    }

    public void setNowsEdts(final Set<NowsEdt> nowsEdts) {
        this.nowsEdts = nowsEdts;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(final Set<Event> events) {
        this.events = events;
    }

    public static SubscriptionBuilder builder() {
        return new SubscriptionBuilder();
    }

    public static final class SubscriptionBuilder {
        private UUID id;
        private String name;
        private boolean active;
        private UUID organisationId;
        private Set<Subscriber> subscribers = new HashSet<>();
        private Set<NowsEdt> nowsEdts = new HashSet<>();
        private Set<Event> events = new HashSet<>();
        private Filter filter;
        private Set<Court> courts = new HashSet<>();

        private SubscriptionBuilder() {
        }

        public SubscriptionBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public SubscriptionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public SubscriptionBuilder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public SubscriptionBuilder withOrganisationId(UUID organisationId) {
            this.organisationId = organisationId;
            return this;
        }

        public SubscriptionBuilder withSubscribers(Set<Subscriber> subscribers) {
            this.subscribers = subscribers;
            return this;
        }

        public SubscriptionBuilder withFilters(Filter filter) {
            this.filter = filter;
            return this;
        }

        public SubscriptionBuilder withCourts(Set<Court> courts) {
            this.courts = courts;
            return this;
        }

        public SubscriptionBuilder withNowsEdts(Set<NowsEdt> nowsEdts) {
            this.nowsEdts = nowsEdts;
            return this;
        }


        public SubscriptionBuilder withEvents(Set<Event> events) {
            this.events = events;
            return this;
        }


        public Subscription build() {
            final Subscription subscription = new Subscription();
            subscription.setId(id);
            subscription.setName(name);
            subscription.setActive(active);
            subscription.setOrganisationId(organisationId);
            subscription.setSubscribers(subscribers);
            subscription.setFilter(filter);
            subscription.setCourts(courts);
            subscription.setNowsEdts(nowsEdts);
            subscription.setEvents(events);
            return subscription;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Subscription that = (Subscription) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }
}
