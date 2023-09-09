package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "deliveryRoutes")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRoute implements PlantSummary {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year and DeliveryRoute num
        return Long.valueOf(String.format("%04d%03d", sale.getSaleYear(), this.num));
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final Long id) {
        // intentionally blank, only needed for Hibernate
    }

    @Override
    @JsonIgnore
    @Transient
    public Integer getCount() {
        return this.orders.stream().mapToInt(Order::getCount).sum();
    }

    @Override
    @JsonIgnore
    @Transient
    public Double getPrice() {
        // sum of all OrderItem prices
        return this.orders.stream().mapToDouble(Order::getPrice).sum();
    }

    @Override
    @JsonIgnore
    @Transient
    public Double getCost() {
        // sum of all OrderItem costs
        return this.orders.stream().mapToDouble(Order::getCost).sum();
    }

    @NonNull
    @NotNull
    @Min(1)
    private Long num;

    @NonNull
    @NotNull
    @Enumerated(EnumType.STRING)
    private DeliveryDay deliveryDay;

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    private Sale sale;

    // TODO: store image of the DeliveryRoute points plotted on a map?
    // private byte[] mapImage;

    @Builder.Default
    @OrderBy("num")
    @Access(AccessType.FIELD)
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "deliveryRoute")
    @ToString.Exclude
    private Set<Order> orders = new TreeSet<>(Comparator.comparingInt(Order::getNum));

    public void addOrder(final Order order) {
        if (order != null) {
            // link DeliveryRoute to Order
            order.setDeliveryRoute(this);

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
        DeliveryRoute that = (DeliveryRoute) o;
        return Objects.equals(num, that.num) && Objects.equals(sale, that.sale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, sale);
    }
}
