package uk.gov.moj.cpp.subscriptions.persistence.entity;

import uk.gov.moj.cpp.subscriptions.persistence.constants.EventType;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "event")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class Event implements Serializable {

    private static final long serialVersionUID = 867848388942237837L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    @Enumerated(EnumType.STRING)
    private EventType name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;


    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public EventType getName() {
        return name;
    }

    public void setName(final EventType name) {
        this.name = name;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
    }

    public static EventBuilder builder() {
        return new EventBuilder();
    }


    public static final class EventBuilder {
        private UUID id;
        private EventType name;
        private Subscription subscription;

        private EventBuilder() {
        }


        public EventBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public EventBuilder withName(final EventType name) {
            this.name = name;
            return this;
        }

        public EventBuilder withSubscription(final Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public Event build() {
            final Event event = new Event();
            event.setId(id);
            event.setName(name);
            event.setSubscription(subscription);
            return event;
        }
    }
}
