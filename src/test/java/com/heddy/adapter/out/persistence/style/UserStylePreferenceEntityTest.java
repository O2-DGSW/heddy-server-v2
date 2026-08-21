package com.heddy.adapter.out.persistence.style;

import com.heddy.domain.style.model.UserStylePreference;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserStylePreferenceEntityTest {

    @Test
    void reportsAssignedIdEntityAsNewUntilPersistedOrLoaded() {
        UserStylePreference preference = UserStylePreference.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UserStylePreference.PreferenceType.PREFERRED);
        UserStylePreferenceEntity entity = new UserStylePreferenceEntity(preference);

        assertThat(entity.getId()).isEqualTo(preference.preferenceId());
        assertThat(entity.isNew()).isTrue();

        entity.markNotNew();

        assertThat(entity.isNew()).isFalse();
    }
}
