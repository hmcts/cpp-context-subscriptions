package uk.gov.moj.cpp.subscriptions.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "court_details")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class Court implements Serializable {

    private static final long serialVersionUID = -2813609388384421101L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "court_id")
    private UUID courtId;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

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

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
    }

    public UUID getCourtId() {
        return courtId;
    }

    public void setCourtId(final UUID courtId) {
        this.courtId = courtId;
    }

    public static CourtsBuilder builder() {
        return new CourtsBuilder();
    }

    public static final class CourtsBuilder {
        private UUID id;
        private UUID courtId;
        private String name;
        private Subscription subscription;

        private CourtsBuilder() {
        }

        public CourtsBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public CourtsBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public CourtsBuilder withSubscription(final Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public CourtsBuilder withCourtId(final UUID courtId) {
            this.courtId = courtId;
            return this;
        }


        public Court build() {
            final Court court = new Court();
            court.setId(id);
            court.setName(name);
            court.setCourtId(courtId);
            court.setSubscription(subscription);
            return court;
        }
    }
}
