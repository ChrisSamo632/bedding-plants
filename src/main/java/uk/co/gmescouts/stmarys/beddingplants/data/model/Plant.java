package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "plants")
@Data
@Builder
@EqualsAndHashCode(of = { "sale", "num" })
@ToString(exclude = { "sale" })
@NoArgsConstructor
@AllArgsConstructor
public class Plant {
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
}
