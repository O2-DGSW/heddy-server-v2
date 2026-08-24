package com.heddy.domain.account.port.in;

import com.heddy.domain.account.model.ConsentDecision;

import java.util.List;

public record EmailSignupCommand(
        String email,
        String password,
        String nickname,
        String phone,
        List<ConsentDecision> agreements
) {
}
