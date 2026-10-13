package com.eventhub.business.abstracts;

import com.eventhub.types.request.LogInRequest;
import com.eventhub.types.request.SignInRequest;
import com.eventhub.types.request.UpdateUserRequest;
import com.eventhub.types.request.ChangePasswordRequest;
import com.eventhub.types.response.AuthTokens;
import com.eventhub.types.output.UserOutput;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;

import java.util.List;
import java.util.UUID;

public interface AuthService {
    Result<String> signIn(SignInRequest signInRequest);
    Result<AuthTokens> logIn(LogInRequest logInRequest);
    /** Refresh cookie'sindeki ham token'ı rotate eder, başarılıysa yeni üçlü token seti döner. */
    Result<AuthTokens> refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
    Result<String> delete(UUID id);
    Result<String> update(UpdateUserRequest request);
    Result<String> changePassword(ChangePasswordRequest request);
    DataResult<List<UserOutput>> getUsers();
}
