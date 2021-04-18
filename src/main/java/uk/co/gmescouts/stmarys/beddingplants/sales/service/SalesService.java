package uk.co.gmescouts.stmarys.beddingplants.sales.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.SaleRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.SaleSummary;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Service
public class SalesService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SalesService.class);

	@Resource
	private SaleRepository saleRepository;

	public Sale saveSale(final Sale sale) {
		LOGGER.info("Saving Sale [{}]", sale.getYear());

		return saleRepository.save(sale);
	}

	public Boolean deleteSale(@NotNull final Integer year) {
		LOGGER.info("Deleting Sale [{}]", year);

		// first check if there is a matching Sale
		final Sale sale = saleRepository.findByYear(year);

		// delete it
		boolean deleted = false;
		if (sale != null) {
			saleRepository.delete(sale);
			deleted = true;
		}

		return deleted;
	}

	public Sale findSaleByYear(@NotNull final Integer year) {
		LOGGER.info("Finding Sale by Year [{}]", year);

		return saleRepository.findByYear(year);
	}

	public Set<Sale> findAllSales() {
		LOGGER.info("Finding all Sales");

		// find and sort by Year
		return new HashSet<>(saleRepository.findAll(Sort.by(Sort.Order.asc("year"))));
	}

	public SaleSummary summariseSale(@NotNull final Sale sale) {
		// count Plants and index by Num for easy access later
		final int plantCount = sale.getPlants().size();

		// count Customers and calculate details about their Orders
		final int customerCount = sale.getCustomers().size();
		final int orderCount = sale.getCustomers().stream().map(Customer::getOrders).mapToInt(Set::size).sum();
		// rounded to 2 d.p.
		final double orderCostTotal = Math.round(
				sale.getCustomers().stream().map(Customer::getOrders).mapToDouble(OrdersService::calculateOrdersCostTotal).sum() * 100.0) / 100.0;
		// rounded to 2 d.p.
		final double orderIncomeTotal = Math.round(
				sale.getCustomers().stream().map(Customer::getOrders).mapToDouble(OrdersService::calculateOrdersIncomeTotal).sum() * 100.0) / 100.0;

		return SaleSummary.builder().year(sale.getYear()).vat(sale.getVat()).plantCount(plantCount).customerCount(customerCount)
				.orderCount(orderCount).orderCostTotal(orderCostTotal).orderIncomeTotal(orderIncomeTotal).build();
	}
}
