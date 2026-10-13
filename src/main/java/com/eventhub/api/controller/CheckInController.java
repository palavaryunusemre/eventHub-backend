package com.eventhub.api.controller;

import com.eventhub.business.abstracts.CheckInService;
import com.eventhub.types.output.CheckInOutput;
import com.eventhub.types.request.CheckInRequest;
import com.eventhub.utilities.result.DataResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.eventhub.config.RestApis.CHECKIN;

@RestController
@RequestMapping(CHECKIN)
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService checkInService;

    @PostMapping("/scan")
    public ResponseEntity<DataResult<CheckInOutput>> scan(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(checkInService.scan(request.getQrToken()));
    }
}
