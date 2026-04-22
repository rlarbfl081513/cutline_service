package com.ssafya408.cutlineparsing.dao;

import com.ssafya408.cutlineparsing.common.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
}
