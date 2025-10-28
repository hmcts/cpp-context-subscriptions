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
@Table(name = "nowsedt")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class NowsEdt implements Serializable {

    private static final long serialVersionUID = -4034504486721574466L;

    @Id
    @Column(name = "id")
    private UUID id;

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

    public static NowsEdtsBuilder builder() {
        return new NowsEdtsBuilder();
    }

    public static final class NowsEdtsBuilder {
        private UUID id;
        private String name;
        private Subscription subscription;

        private NowsEdtsBuilder() {
        }

        public NowsEdtsBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public NowsEdtsBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public NowsEdtsBuilder withSubscription(final Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public NowsEdt build() {
            final NowsEdt nowsEdt = new NowsEdt();
            nowsEdt.setId(id);
            nowsEdt.setName(name);
            nowsEdt.setSubscription(subscription);
            return nowsEdt;
        }
    }
}
