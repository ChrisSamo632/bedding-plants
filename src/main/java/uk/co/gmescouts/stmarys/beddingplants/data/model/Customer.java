package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "customers")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @JsonIgnore
    @Id
    public String getId() {
        return String.format("%s-%04d", this.getName().toUpperCase(Locale.ROOT), sale.getSaleYear());
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    public void setId(final String id) {
        // intentionally blank, for Entity/Jackson construction only
    }

    @JsonIgnore
    @Transient
    public String getName() {
        return String.format("%s %s", forename, surname);
    }

    @NonNull
    @NotNull
    private String forename;

    @NonNull
    @NotNull
    private String surname;

    @Access(AccessType.FIELD)
    // don't delete the Address just because the Customer is being removed (may belong to another Customer)
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    private Address address;

    private String emailAddress;

    private String telephone;

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    private Sale sale;

    @NonNull
    @Builder.Default
    @Access(AccessType.FIELD)
    @OrderBy("num")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "customer")
    @ToString.Exclude
    private Set<Order> orders = new TreeSet<>(Comparator.comparingInt(Order::getNum));

    public void addOrder(final Order order) {
        if (order != null) {
            // link Customer to Order
            order.setCustomer(this);

            // replace existing Order, if present
            orders.add(order);
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
        Customer customer = (Customer) o;
        return Objects.equals(forename, customer.forename) && Objects.equals(surname, customer.surname) && Objects.equals(sale, customer.sale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forename, surname, sale);
    }
}
