package uk.co.gmescouts.stmarys.beddingplants.data;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;

import javax.persistence.OrderBy;
import java.util.Set;

public interface PlantRepository extends JpaRepository<Plant, Long> {
	Plant findByNumAndSaleYear(Integer num, Integer saleYear);

	@OrderBy("num")
	Set<Plant> findBySaleYear(Integer saleYear);
}
