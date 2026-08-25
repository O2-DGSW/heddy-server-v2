package com.heddy.domain.file.model;

/**
 * 오브젝트 스토리지에 실제로 올라가 있는 객체의 사실. 선언값이 아니라 스토리지가 알려준 값이다.
 *
 * @param contentType 스토리지에 기록된 Content-Type
 * @param byteSize    실제 크기
 */
public record StorageObject(String contentType, long byteSize) {
}
