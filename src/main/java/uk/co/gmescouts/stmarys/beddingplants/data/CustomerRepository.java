package uk.co.gmescouts.stmarys.beddingplants.data;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;

import javax.persistence.OrderBy;
import java.util.Set;

public interface CustomerRepository extends JpaRepository<Customer, String> {
	@OrderBy("surname, forename")
	Set<Customer> findBySaleYear(Integer saleYear);

	Customer findByEmailAddress(Integer emailAddress);
}
