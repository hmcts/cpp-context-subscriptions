package uk.gov.moj.cpp.subscriptions.persistence.repository;

import uk.gov.moj.cpp.subscriptions.persistence.entity.Subscription;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SubscriptionsRepository extends EntityRepository<Subscription, UUID> {

    List<Subscription> findByOrganisationId(final UUID organisationId);

    @Query(value = "select s.* from Subscription s where exists(select 1 from court_details c where c.court_id = ?1 " +
            "and s.id = c.subscription_id)", isNative = true)
    List<Subscription> findByCourtId(final UUID courtId);

    @Query(value = "select s.* from Subscription s where s.organisation_id = :organisationId and exists(select 1 from subscriber sub where sub.email_address = :emailAddress " +
            "and s.id = sub.subscription_id)", isNative = true)
    List<Subscription> findByOrganisationIdAndSubscriber(@QueryParam("organisationId") final UUID organisationId, @QueryParam("emailAddress") final String emailAddress);
}
