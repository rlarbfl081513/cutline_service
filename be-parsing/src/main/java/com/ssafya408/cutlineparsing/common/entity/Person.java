package com.ssafya408.cutlineparsing.common.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
	private PersonRelation relation;

	@Column(name = "duration", nullable = false)
	private Integer duration;

	@Column(name = "total_give")
	private Integer totalGive;

	@Column(name = "total_take")
	private Integer totalTake;

	@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Topic> topics = new ArrayList<>();

	@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Issue> issues = new ArrayList<>();

	@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PersonValue> personValues = new ArrayList<>();

	public Person(User user, String name, LocalDate birth, Gender gender, PersonStatus status, PersonRelation relation, Integer duration) {
		this.user = user;
		this.name = name;
		this.birth = birth;
		this.gender = gender;
		this.status = status;
		this.relation = relation;
		this.duration = duration;
	}
	
	public Person update(String name, LocalDate birth, Gender gender, PersonStatus status, PersonRelation relation, Integer duration) {
		this.name = name;
		this.birth = birth;
		this.gender = gender;
		this.status = status;
		this.relation = relation;
		this.duration = duration;
		return this;
	}
	
	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public void addTopic(Topic topic) {
		topics.add(topic);
	}

	public void addIssue(Issue issue) {
		issues.add(issue);
	}

	public void addPersonValue(PersonValue personValue) {
		personValues.add(personValue);
	}

	public void updateStrategies(String interest, String uninterest, String maintain) {
		this.interestStrategy = interest;
		this.uninterestStrategy = uninterest;
		this.maintainStrategy = maintain;
	}
}
