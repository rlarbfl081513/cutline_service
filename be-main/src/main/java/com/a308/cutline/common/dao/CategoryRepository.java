package com.a308.cutline.common.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Title;

import java.util.Optional;


public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByTitle(Title title);
}
