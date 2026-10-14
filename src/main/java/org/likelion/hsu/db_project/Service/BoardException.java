package org.likelion.hsu.db_project.Service;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 게시판 요청 오류. PostController에서 { "message": ... } JSON으로 응답한다.
 */
@Getter
public class BoardException extends RuntimeException {

    private final HttpStatus status;

    public BoardException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
