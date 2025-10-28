package uk.gov.moj.cpp.subscriptions.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "subscriber")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class Subscriber implements Serializable {

    private static final long serialVersionUID = 2852153649774475146L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "active")
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
    }

    public static SubscribersBuilder builder() {
        return new SubscribersBuilder();
    }

    public static final class SubscribersBuilder {
        private UUID id;
        private String emailAddress;
        private boolean active;
        private Subscription subscription;

        private SubscribersBuilder() {
        }


        public SubscribersBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public SubscribersBuilder withEmailAddress(final String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public SubscribersBuilder withActive(final boolean active) {
            this.active = active;
            return this;
        }

        public SubscribersBuilder withSubscription(final Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public Subscriber build() {
            final Subscriber subscriber = new Subscriber();
            subscriber.setId(id);
            subscriber.setEmailAddress(emailAddress);
            subscriber.setActive(active);
            subscriber.setSubscription(subscription);
            return subscriber;
        }
    }
}
