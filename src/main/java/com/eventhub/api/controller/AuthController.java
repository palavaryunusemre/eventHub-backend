package com.eventhub.api.controller;

import com.eventhub.business.abstracts.AuthService;
import com.eventhub.services.JwtService;
import com.eventhub.types.output.UserOutput;
import com.eventhub.types.request.LogInRequest;
import com.eventhub.types.request.SignInRequest;
import com.eventhub.types.request.UpdateUserRequest;
import com.eventhub.types.request.ChangePasswordRequest;
import com.eventhub.types.response.AuthTokens;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.eventhub.config.RestApis.*;
@RestController
@RequestMapping(AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    @PostMapping(SIGN_IN)
    public ResponseEntity<Result<String>> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        return ResponseEntity.ok(this.authService.signIn(signInRequest));
    }

    @PutMapping("/update")
    public ResponseEntity<Result<String>> update(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(this.authService.update(request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Result<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(this.authService.changePassword(request));
    }

    @PostMapping(LOG_IN)
    public ResponseEntity<Result<String>> logIn(@Valid @RequestBody LogInRequest logInRequest) {
        Result<AuthTokens> result = this.authService.logIn(logInRequest);
        return withTokenCookies(ResponseEntity.ok(), result);
    }
    @PostMapping(REFRESH)
    public ResponseEntity<Result<String>> refresh(
            @CookieValue(name = JwtService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        Result<AuthTokens> result = this.authService.refresh(refreshToken);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, jwtService.clearAuthCookie().toString())
                    .header(HttpHeaders.SET_COOKIE, jwtService.clearInfoCookie().toString())
                    .header(HttpHeaders.SET_COOKIE, jwtService.clearRefreshCookie().toString())
                    .body(new Result<>(false, result.getCode(), result.getMessage()));
        }
        return withTokenCookies(ResponseEntity.ok(), result);
    }

    @PostMapping(LOG_OUT)
    public ResponseEntity<Result<String>> logout(
            @CookieValue(name = JwtService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        this.authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearAuthCookie().toString())
                .header(HttpHeaders.SET_COOKIE, jwtService.clearInfoCookie().toString())
                .header(HttpHeaders.SET_COOKIE, jwtService.clearRefreshCookie().toString())
                .body(new Result<>(true, 200, "Çıkış yapıldı."));
    }

    @DeleteMapping(DELETE_USER)
    public ResponseEntity<Result<String>> delete(@RequestParam UUID id){
        return ResponseEntity.ok(this.authService.delete(id));
    }

    @GetMapping(GET_USERS)
    public ResponseEntity<DataResult<List<UserOutput>>> getUsers() {
        return ResponseEntity.ok(this.authService.getUsers());
    }
    private ResponseEntity<Result<String>> withTokenCookies(ResponseEntity.BodyBuilder response, Result<AuthTokens> result) {
        if (result.isSuccess() && result instanceof DataResult<AuthTokens> dataResult && dataResult.getData() != null) {
            AuthTokens tokens = dataResult.getData();
            response.header(HttpHeaders.SET_COOKIE, jwtService.buildAuthCookie(tokens.getAuthToken()).toString());
            response.header(HttpHeaders.SET_COOKIE, jwtService.buildInfoCookie(tokens.getInfoToken()).toString());
            response.header(HttpHeaders.SET_COOKIE, jwtService.buildRefreshCookie(tokens.getRefreshToken()).toString());
        }
        Result<String> body = new Result<>(result.isSuccess(), result.getCode(), result.getMessage());
        return response.body(body);
    }
}
