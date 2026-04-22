package com.a308.cutline.domain.person.api;

import com.a308.cutline.common.dto.ApiResponse;
import com.a308.cutline.domain.person.dao.PersonWithLatestValueResponse;
import com.a308.cutline.domain.person.dto.PersonCreateRequest;
import com.a308.cutline.domain.person.dto.PersonDetailResponse;
import com.a308.cutline.domain.person.dto.PersonResponse;
import com.a308.cutline.domain.person.dto.PersonUpdateRequest;
import com.a308.cutline.domain.person.service.PersonService;
import com.a308.cutline.util.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/people")
// 이건 알아서 찾아봐~ final 이게 마지막이다...개념을
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<PersonResponse>> createPerson(
            @RequestBody PersonCreateRequest request) {
        try {
            Long userId = AuthenticationUtils.getCurrentUserId();
            PersonResponse response = personService.createPerson(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<PersonResponse>>> getPersons() {
//        Long userId = AuthenticationUtils.getCurrentUserId();
//        List<PersonResponse> responses = personService.findPersonsByUser(userId);
//        return ResponseEntity.ok(ApiResponse.success(responses));
//    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonWithLatestValueResponse>>> getPersonsWithLatest() {
        Long userId = AuthenticationUtils.getCurrentUserId();
        List<PersonWithLatestValueResponse> responses = personService.findPersonsWithLatestByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonDetailResponse>> getPerson(
            @PathVariable Long id
    ) {
        try {
            PersonDetailResponse res = personService.findPersonDetail(id);
            return ResponseEntity.ok(ApiResponse.success(res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonResponse>> updatePerson(
            @PathVariable Long id,
            @RequestBody PersonUpdateRequest request) {
        try {
            PersonResponse response = personService.updatePerson(id, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePerson(
            @PathVariable Long id) {
            try {
                personService.softDelete(id);
                return ResponseEntity.noContent().build(); // 되면 204 나옴
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
            }
        }


    }