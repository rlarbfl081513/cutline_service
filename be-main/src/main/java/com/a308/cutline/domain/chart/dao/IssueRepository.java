package com.a308.cutline.domain.chart.dao;

import com.a308.cutline.domain.chart.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findAllByPersonIdOrderByCreatedAtDesc(Long personId);
}
