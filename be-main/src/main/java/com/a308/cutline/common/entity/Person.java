package com.a308.cutline.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Person.java 파일 상단에 추가
import com.a308.cutline.common.entity.Gender;
import com.a308.cutline.common.entity.Relation;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "person")
public class Person extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "person_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "name", length = 100)
	private String name;

	@Column(name = "age")
	private Integer age;

	@Column(name = "birth")
	private LocalDate birth;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", length = 6)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 12)
	private PersonStatus status;

	@Column(name = "interest_strategy", length = 300)
	private String interestStrategy;

	@Column(name = "uninterest_strategy", length = 300)
	private String uninterestStrategy;

	@Column(name = "maintain_strategy", length = 300)
	private String maintainStrategy;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "relation", length = 20)
	private Relation relation;

	@Column(name = "duration", nullable = false)
	private Integer duration;

	@Column(name = "total_give")
	private Integer totalGive;

	@Column(name = "total_take")
	private Integer totalTake;

	public Person( User user, String name, Integer age, LocalDate birth, Gender gender, PersonStatus status, Relation relation, Integer duration) {
		this.user = user;
		this.name = name;
		this.birth = birth;
		this.gender = gender;
		this.status = status;
		this.relation = relation;
		this.duration = duration;
		this.age = age;
	}

	public Person update( String name, Integer age, LocalDate birth, Gender gender, PersonStatus status, Relation relation, Integer duration) {
		this.name = name;
		this.birth = birth;
		this.gender = gender;
		this.status = status;
		this.relation = relation;
		this.duration = duration;
		this.age = age;
		return this;
	}
	
	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

    public void addGive(int amount) {
        if (amount <= 0) return;
        this.totalGive = (this.totalGive == null ? 0 : this.totalGive) + amount;
    }
    public void addTake(int amount) {
        if (amount <= 0) return;
        this.totalTake = (this.totalTake == null ? 0 : this.totalTake) + amount;
    }
}