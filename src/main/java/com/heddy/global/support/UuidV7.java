package com.heddy.global.support;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.UUID;

/**
 * UUIDv7 생성기. 앞쪽 48비트가 Unix epoch 밀리초라 시간순으로 정렬돼 인덱스 삽입 비용이 낮다.
 * 모든 테이블의 PK 는 DB 기본값이 아니라 애플리케이션에서 이 유틸로 생성한다.
 */
public final class UuidV7 {

    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    private UuidV7() {
    }

    public static UUID generate() {
        return GENERATOR.generate();
    }
}
