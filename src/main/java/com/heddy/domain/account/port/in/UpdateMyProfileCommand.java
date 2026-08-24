package com.heddy.domain.account.port.in;

import java.util.UUID;

public record UpdateMyProfileCommand(
        UUID userId,
        boolean nicknamePresent,
        String nickname,
        boolean phonePresent,
        String phone,
        boolean preferredDesignerPresent,
        String preferredDesigner,
        boolean hairCautionsPresent,
        String hairCautions
) {
}
