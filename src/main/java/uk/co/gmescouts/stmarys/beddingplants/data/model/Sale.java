package uk.co.gmescouts.stmarys.beddingplants.data.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "sales")
public class Sale {
    @NotNull
    @Id
    @Column(unique = true)
    private Integer saleYear;

    @Min(0)
    @NotNull
    private Double vat;

    @Min(0)
    @NotNull
    private Double deliveryCharge;

    @SuppressWarnings("null")
    @OrderBy("num")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "sale")
    private Set<DeliveryRoute> deliveryRoutes = new TreeSet<>(Comparator.comparing(DeliveryRoute::getNum));

    @SuppressWarnings("null")
    @OrderBy("surname, forename")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "sale")
    private Set<Customer> customers = new TreeSet<>(Comparator.comparing(Customer::getName));

    @SuppressWarnings("null")
    @OrderBy("num")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "sale")
    private Set<Plant> plants = new TreeSet<>(Comparator.comparingInt(Plant::getNum));

    public void addCustomer(final Customer customer) {
        if (customer != null) {
            // link Sale to Customer
            customer.setSale(this);

            // replace existing Customer, if present
            customers.add(customer);
        }
    }

    public void addPlant(final Plant plant) {
        if (plant != null) {
            // link Sale to Plant
            plant.setSale(this);

            // replace existing Plant, if present
            plants.add(plant);
        }
    }

    public void addDeliveryRoute(final DeliveryRoute deliveryRoute) {
        if (deliveryRoute != null) {
            // link Sale to DeliveryRoute
            deliveryRoute.setSale(this);

            // replace existing DeliveryRoute, if present
            deliveryRoutes.add(deliveryRoute);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sale sale = (Sale) o;
        return Objects.equals(saleYear, sale.saleYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleYear);
    }

    public Sale() {
        // required by JPA and Jackson
    }

    public Sale(final Integer saleYear, final Double vat, final Double deliveryCharge) {
        this.saleYear = Objects.requireNonNull(saleYear, "saleYear");
        this.vat = Objects.requireNonNull(vat, "vat");
        this.deliveryCharge = Objects.requireNonNull(deliveryCharge, "deliveryCharge");
    }

    public Integer getSaleYear() {
        return saleYear;
    }

    public void setSaleYear(final Integer saleYear) {
        this.saleYear = Objects.requireNonNull(saleYear, "saleYear");
    }

    public Double getVat() {
        return vat;
    }

    public void setVat(final Double vat) {
        this.vat = Objects.requireNonNull(vat, "vat");
    }

    public Double getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(final Double deliveryCharge) {
        this.deliveryCharge = Objects.requireNonNull(deliveryCharge, "deliveryCharge");
    }

    public Set<DeliveryRoute> getDeliveryRoutes() {
        return deliveryRoutes;
    }

    public void setDeliveryRoutes(final Set<DeliveryRoute> deliveryRoutes) {
        this.deliveryRoutes = Objects.requireNonNull(deliveryRoutes, "deliveryRoutes");
    }

    public Set<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(final Set<Customer> customers) {
        this.customers = Objects.requireNonNull(customers, "customers");
    }

    public Set<Plant> getPlants() {
        return plants;
    }

    public void setPlants(final Set<Plant> plants) {
        this.plants = Objects.requireNonNull(plants, "plants");
    }

    @Override
    public String toString() {
        return "Sale(" + "saleYear=" + saleYear + ", " + "vat=" + vat + ", " + "deliveryCharge=" + deliveryCharge + ")";
    }
}
