package com.a308.cutline.domain.familiy_event.controller;


import com.a308.cutline.common.dto.ApiResponse;
import com.a308.cutline.domain.familiy_event.dto.FamilyEventCreateRequest;
import com.a308.cutline.domain.familiy_event.dto.FamilyEventResponse;
import com.a308.cutline.domain.familiy_event.service.FamilyEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/people/{personId}/family-events")
@RequiredArgsConstructor
public class FamilyEventController {

    private final FamilyEventService familyEventService;

    @PostMapping
    public ResponseEntity<ApiResponse<FamilyEventResponse>> createFamilyEvent(@RequestBody FamilyEventCreateRequest request){
        FamilyEventResponse responses = FamilyEventResponse.from(familyEventService.createFamilyEvent(request));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // READ ALL: 모든 이벤트를 조회하는 GET 요청
    @GetMapping("/All")
    public ResponseEntity<List<FamilyEventResponse>> getAllFamilyEvents(@PathVariable("personId") Long id) {
        List<FamilyEventResponse> events = familyEventService.getAllFamilyEvents(id);
        return ResponseEntity.ok(events);
    }



}
