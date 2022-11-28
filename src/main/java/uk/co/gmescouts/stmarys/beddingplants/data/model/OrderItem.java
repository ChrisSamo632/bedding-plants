package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Table(name = "orderitems")
@Data
@Builder
@EqualsAndHashCode(exclude = {"count"})
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem implements PlantSummary {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year, Order num and Plant num
        return Long.valueOf(String.format("%d%03d%02d", order.getCustomer().getSale().getSaleYear(), order.getNum(), plant.getNum()));
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final Long id) {
        // intentionally blank, only needed for Hibernate
    }

    @Override
    @JsonIgnore
    @Transient
    public Double getPrice() {
        // plant price inc. VAT * number of plants ordered
        return this.plant.getPrice() * this.count;
    }

    @Override
    @JsonIgnore
    @Transient
    public Double getCost() {
        // (plant cost exc. VAT + VAT) * number of plants ordered
        final double vatMultiplier = 1.0 + (this.plant.getSale().getVat() / 100.0);

		return this.plant.getCost() * vatMultiplier * this.count;
	}

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    @ToString.Exclude
    private Order order;

    @NonNull
    @Access(AccessType.FIELD)
    @ManyToOne
    private Plant plant;

    @NonNull
    @Min(1)
    @NotNull
    private Integer count;
}
