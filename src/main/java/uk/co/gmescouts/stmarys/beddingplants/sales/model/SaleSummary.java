package uk.co.gmescouts.stmarys.beddingplants.sales.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class SaleSummary {
	@NonNull
	private Integer year;

	@NonNull
	private Double vat;

	@NonNull
	private Double deliveryCharge;

	@NonNull
	private Integer plantCount;

	@NonNull
	private Integer customerCount;

	@NonNull
	private Integer orderCount;

	@NonNull
	private Double orderCostTotal;

	@NonNull
	private Double orderIncomeTotal;

	@NonNull
	private Integer orderPlantTotal;
}
