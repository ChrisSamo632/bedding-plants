package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "plants")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plant implements Comparable<Plant> {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year and Plant num
        return Long.valueOf(String.format("%d%02d", sale.getSaleYear(), this.num));
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final Long id) {
        // intentionally blank, only needed for Hibernate
    }

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    @ToString.Exclude
    private Sale sale;

    @NonNull
    @Min(1)
    @NotNull
    private Integer num;

    @NonNull
    @NotNull
    private String name;

    @NonNull
    @NotNull
    private String variety;

    private String details;

    @NonNull
    @Min(0)
    @NotNull
    private Double price;

    @NonNull
    @Min(0)
    @NotNull
    private Double cost;

    @Access(AccessType.FIELD)
    @OneToMany(mappedBy = "plant")
    @ToString.Exclude
    private Set<OrderItem> orderItems;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Plant plant = (Plant) o;
        return Objects.equals(sale, plant.sale) && Objects.equals(num, plant.num);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sale, num);
    }

    @Override
    public int compareTo(@org.jetbrains.annotations.NotNull final Plant o) {
        return this.getNum().compareTo(o.getNum());
    }
}
