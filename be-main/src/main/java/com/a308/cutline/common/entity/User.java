package com.a308.cutline.common.entity;

import com.a308.cutline.common.entity.BaseEntity;
import com.a308.cutline.common.entity.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(name = "email", length = 100, unique = true)
	private String email;

	@Column(name = "name", length = 200)
	private String name;

	@Column(name = "birth")
	private LocalDate birth;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", length = 10)
	private Gender gender;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public User(String email, String name, LocalDate birth, Gender gender) {
		this.email = email;
		this.name = name;
		this.birth = birth;
		this.gender = gender;
	}

	public void setBirth(LocalDate birth) {
		this.birth = birth;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setName(String name) {
		this.name = name;
	}
}