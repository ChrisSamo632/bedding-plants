package uk.co.gmescouts.stmarys.beddingplants.sales.model;

import java.util.Objects;

public record SaleSummary(Integer year, Double vat, Double deliveryCharge, Integer plantCount, Integer customerCount, Integer orderCount,
		Double orderCostTotal, Double orderIncomeTotal, Integer orderPlantTotal) {
	public SaleSummary {
		Objects.requireNonNull(year, "year");
		Objects.requireNonNull(vat, "vat");
		Objects.requireNonNull(deliveryCharge, "deliveryCharge");
		Objects.requireNonNull(plantCount, "plantCount");
		Objects.requireNonNull(customerCount, "customerCount");
		Objects.requireNonNull(orderCount, "orderCount");
		Objects.requireNonNull(orderCostTotal, "orderCostTotal");
		Objects.requireNonNull(orderIncomeTotal, "orderIncomeTotal");
		Objects.requireNonNull(orderPlantTotal, "orderPlantTotal");
	}
}
