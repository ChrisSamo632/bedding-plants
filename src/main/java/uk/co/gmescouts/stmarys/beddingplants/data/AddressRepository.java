package uk.co.gmescouts.stmarys.beddingplants.data;

import jakarta.persistence.OrderBy;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Address;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;

import java.util.Set;

public interface AddressRepository extends JpaRepository<Address, String> {
    @OrderBy("city, town, street, houseNameNumber")
    Set<Address> findAddressByCustomersSaleSaleYear(Integer customersSaleSaleYear);

    @OrderBy("city, town, street, houseNameNumber")
    Set<Address> findAddressByCustomersSaleSaleYearAndCustomersOrdersType(Integer customersSaleSaleYear, OrderType customersOrdersType);
}
