package uk.co.gmescouts.stmarys.beddingplants.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;

import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, Long> {
	Order findByNumAndCustomerSaleSaleYear(Integer num, Integer customerSaleSaleYear);

	Set<Order> findByTypeAndCustomerSaleSaleYear(OrderType type, Integer customerSaleSaleYear, Sort sort);

	Set<Order> findByCustomerSaleSaleYear(Integer customerSaleSaleYear, Sort sort);
}
