package com.a308.cutline.domain.person.service;

import com.a308.cutline.common.entity.Person;
import com.a308.cutline.domain.person.dao.PersonWithLatestValueResponse;
import com.a308.cutline.domain.person.dto.PersonCreateRequest;
import com.a308.cutline.domain.person.dto.PersonResponse;
import com.a308.cutline.domain.person.dto.PersonUpdateRequest;
import com.a308.cutline.domain.person.dao.PersonRepository;
import com.a308.cutline.common.entity.User;
import com.a308.cutline.domain.personvalue.dao.LatestPersonValueProjection;
import com.a308.cutline.domain.personvalue.dao.PersonValueRepository;
import com.a308.cutline.domain.user.dao.UserRepository;
import com.a308.cutline.domain.chart.dao.ChatManualStatsRepository;
import com.a308.cutline.domain.chart.dto.ChatManualStatsResponse;
import com.a308.cutline.domain.chart.entity.ChatManualStats;
import com.a308.cutline.domain.person.dto.PersonDetailResponse;
import com.a308.cutline.domain.person.dao.TopicRepository;
import com.a308.cutline.domain.person.dto.TopicResponse;
import com.a308.cutline.common.entity.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final ChatManualStatsRepository chatManualStatsRepository;
    private final TopicRepository topicRepository;
    private final PersonValueRepository personValueRepository;

//  데이터 생성할 때, 업데이트/딜리트 할 때 -> 메서드 실행? 트랜잭션 : 롤백
    @Transactional
    public PersonResponse createPerson(Long userId, PersonCreateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Person person = new Person(
            user,
            request.getName(),
            request.getAge(),
            request.getBirth(),
            request.getGender(),
            request.getStatus(),
            request.getRelation(),
            request.getDuration()
        );

        Person savedPerson = personRepository.save(person);
        return PersonResponse.from(savedPerson);
    }

    public List<PersonResponse> findPersonsByUser(Long userId) {
        return personRepository.findByUserIdAndDeletedAtIsNull(userId)
            .stream()
            .map(PersonResponse::from)
            .collect(Collectors.toList());
    }

    public PersonResponse findPerson(Long personId) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 인물입니다."));
        return PersonResponse.from(person);
    }

    @Transactional
    public PersonResponse updatePerson(Long personId, PersonUpdateRequest request) {
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 인물입니다."));

        // Person 엔티티에 업데이트 메서드 추가 필요
        Person updatedPerson = person.update(
            request.getName(),
            request.getAge(),
            request.getBirth(),
            request.getGender(),
            request.getStatus(),
            request.getRelation(),
            request.getDuration()
        );

        return PersonResponse.from(updatedPerson);
    }

    @Transactional
    public void softDelete(Long personId) {
        Person person = personRepository
                .findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없거나 이미 삭제"));
        person.softDelete();
    }

    @Transactional(readOnly = true)
    public PersonDetailResponse findPersonDetail(Long personId) {
        var person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 person"));

        // 기본 PersonResponse
        PersonResponse personResp = PersonResponse.from(person);

        // 최신 ChatManualStats 1건 (없으면 null)
        ChatManualStatsResponse latestManual =
                chatManualStatsRepository.findLatestByPersonId(personId)
                        .map(ChatManualStatsResponse::from)
                        .orElse(null);

        // Topic 전체 리스트 (최신순 권장)
        // 🔹 Topic TOP5 (현재 연/월 기준)
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        var topics = topicRepository
                .findTop3TopicsByConditions(personId, currentYear, currentMonth)
                .stream()
                .map(TopicResponse::from)
                .toList();
        return PersonDetailResponse.of(personResp, latestManual, topics);
    }

        // 기존 findPersonsByUser(Long) 그대로 둠

        public List<PersonWithLatestValueResponse> findPersonsWithLatestByUser(Long userId) {
            List<Person> persons = personRepository.findByUserIdAndDeletedAtIsNull(userId);
            if (persons.isEmpty()) return List.of();

            List<Long> ids = persons.stream().map(Person::getId).toList();

            Map<Long, LatestPersonValueProjection> latestMap =
                    personValueRepository.findLatestByPersonIds(ids).stream()
                            .collect(Collectors.toMap(LatestPersonValueProjection::getPersonId, p -> p));

            return persons.stream()
                    .map(p -> {
                        PersonResponse base = PersonResponse.from(p); // ✅ 기존 팩토리 그대로 사용
                        LatestPersonValueProjection li = latestMap.get(p.getId());
                        return new PersonWithLatestValueResponse(
                                base,
                                li != null ? li.getValue() : null,
                                li != null ? li.getYear()  : null,
                                li != null ? li.getMonth() : null,
                                li != null && li.getChangeRate() != null ? li.getChangeRate().floatValue() : null
                        );
                    })
                    .toList();
        }
    }

