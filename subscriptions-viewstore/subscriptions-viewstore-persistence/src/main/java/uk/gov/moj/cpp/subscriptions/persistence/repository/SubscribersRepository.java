package uk.gov.moj.cpp.subscriptions.persistence.repository;

import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscriber;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SubscribersRepository extends EntityRepository<Subscriber, UUID> {

    @Query(value = "from Subscriber subscriber where subscriber.emailAddress =  ?1 and subscriber.subscription.id = ?2")
    Subscriber findSubscriberByEmailAddressForSubscription(final String emailAddress, final UUID subscriptionId);

}
