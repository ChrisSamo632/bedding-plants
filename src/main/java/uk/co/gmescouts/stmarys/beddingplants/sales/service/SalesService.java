package uk.co.gmescouts.stmarys.beddingplants.sales.service;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.SaleRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.SaleSummary;

import java.util.List;
import java.util.Set;

@Service
public class SalesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesService.class);

    @Resource
    private SaleRepository saleRepository;

    public Sale saveSale(final Sale sale) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Saving Sale [{}]", sale.getSaleYear());
        }

        return saleRepository.save(sale);
    }

    public Boolean deleteSale(@NotNull final Integer year) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Deleting Sale [{}]", year);
        }

        // first check if there is a matching Sale
        final Sale sale = saleRepository.findBySaleYear(year);

        // delete it
        boolean deleted = false;
        if (sale != null) {
            saleRepository.delete(sale);
            deleted = true;
        }

        return deleted;
    }

    public Sale findSaleByYear(@NotNull final Integer year) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Finding Sale by Year [{}]", year);
        }

        return saleRepository.findBySaleYear(year);
    }

    public List<Sale> findAllSales() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Finding all Sales");
        }

        // find and sort by Year
        return saleRepository.findAll(Sort.by(Sort.Order.desc("saleYear")));
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
        final int orderPlantTotal = sale.getCustomers().stream().map(Customer::getOrders).flatMapToInt(orders -> orders.stream().mapToInt(Order::getCount)).sum();

        return SaleSummary.builder().year(sale.getSaleYear()).vat(sale.getVat()).deliveryCharge(sale.getDeliveryCharge()).plantCount(plantCount).customerCount(customerCount)
                .orderCount(orderCount).orderCostTotal(orderCostTotal).orderIncomeTotal(orderIncomeTotal).orderPlantTotal(orderPlantTotal).build();
    }
}
