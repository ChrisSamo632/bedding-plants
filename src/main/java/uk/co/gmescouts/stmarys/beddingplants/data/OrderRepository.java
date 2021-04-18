package uk.co.gmescouts.stmarys.beddingplants.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;

import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, Long> {
	Order findByNumAndCustomerSaleYear(Integer num, Integer customerSaleYear);

	Set<Order> findByTypeAndCustomerSaleYear(OrderType type, Integer customerSaleYear, Sort sort);

	Set<Order> findByCustomerSaleYear(Integer customerSaleYear, Sort sort);
}
