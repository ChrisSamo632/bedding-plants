package uk.co.gmescouts.stmarys.beddingplants.data;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long> {
	Sale findByYear(Integer year);
}
