package org.likelion.hsu.db_project.Service;

/**
 * 공공데이터 API를 쓸 수 없을 때 (키 미설정, 호출 실패 등)
 */
public class ShelterException extends RuntimeException {
    public ShelterException(String message) {
        super(message);
    }
}
