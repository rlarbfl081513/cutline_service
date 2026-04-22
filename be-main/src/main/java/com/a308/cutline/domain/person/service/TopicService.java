package com.a308.cutline.domain.person.service;

import com.a308.cutline.domain.person.dao.TopicRepository;
import org.springframework.stereotype.Service;
import com.a308.cutline.common.entity.Topic;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TopicService {
    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository){
        this.topicRepository = topicRepository;
    }

    //현재 달에서 카운트가 가장 많은 topic 5개를 추출한다
    public  List<Topic>  getMostCountTopicInCurrentMonth(Long personId){

        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        return topicRepository
                .findTop3TopicsByConditions(personId, currentYear, currentMonth);
    }
}
