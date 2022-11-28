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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "sales")
@Data
@Builder
@EqualsAndHashCode(of = "saleYear")
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
    @NonNull
    @NotNull
    @Id
    @Column(unique = true)
    private Integer saleYear;

    @NonNull
    @Min(0)
    @NotNull
    private Double vat;

    @NonNull
    @Min(0)
    @NotNull
    private Double deliveryCharge;

    @NonNull
    @Builder.Default
    @OrderBy("num")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "sale")
    private Set<DeliveryRoute> deliveryRoutes = new TreeSet<>(Comparator.comparing(DeliveryRoute::getNum));

    @NonNull
    @Builder.Default
    @OrderBy("surname, forename")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "sale")
    private Set<Customer> customers = new TreeSet<>(Comparator.comparing(Customer::getName));

    @NonNull
    @Builder.Default
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
}
