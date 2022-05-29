package uk.co.gmescouts.stmarys.beddingplants.data;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;

import javax.persistence.OrderBy;
import java.util.Set;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, Long> {
	@OrderBy("num")
	Set<DeliveryRoute> findDeliveryRouteBySaleSaleYear(Integer saleSaleYear);

	DeliveryRoute findDeliveryRouteBySaleSaleYearAndNum(Integer saleSaleYear, Long num);
}
