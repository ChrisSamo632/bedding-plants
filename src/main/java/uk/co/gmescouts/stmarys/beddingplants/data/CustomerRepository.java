package uk.co.gmescouts.stmarys.beddingplants.data;

import jakarta.persistence.OrderBy;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;

import java.util.Set;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    @OrderBy("surname, forename")
    Set<Customer> findBySaleSaleYear(Integer saleSaleYear);

    Customer findByEmailAddress(Integer emailAddress);
}
