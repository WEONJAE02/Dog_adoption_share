package org.likelion.hsu.db_project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.likelion.hsu.db_project.Dto.MyPetRequestDto;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.likelion.hsu.db_project.Repository.MyPetRepository;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MyPetServiceTest {

    private MyPetRepository petRepository;
    private ImageStorage imageStorage;
    private HealthCheckService healthCheckService;
    private MyPetService service;

    @BeforeEach
    void setUp() {
        petRepository = mock(MyPetRepository.class);
        imageStorage = mock(ImageStorage.class);
        healthCheckService = mock(HealthCheckService.class);
        service = new MyPetService(petRepository, imageStorage, healthCheckService);
        when(petRepository.findById(10L)).thenReturn(Optional.of(MyPet.builder().petId(10L).userId(1L).name("콩이").species("개").imageName("old.jpg").build()));
    }

    @Test
    void 다른_사람의_반려동물은_보거나_지울_수_없다() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BoardException.class, () -> service.getPet(10L, 2L)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BoardException.class, () -> service.deletePet(10L, 2L)).getStatus());
        verify(petRepository, never()).delete(any());
    }

    @Test
    void 반려동물을_지우면_건강체크_기록과_사진도_지운다() {
        service.deletePet(10L, 1L);

        verify(healthCheckService).deleteAllByPet(10L);
        verify(petRepository).delete(10L);
        verify(imageStorage).delete("old.jpg");
    }

    @Test
    void 열_마리를_넘게_등록할_수_없다() {
        when(petRepository.countByUserId(1L)).thenReturn(10);

        assertThrows(BoardException.class, () -> service.createPet(pet("보리", "개", null, null), null, 1L));
        verify(petRepository, never()).save(any());
    }

    @Test
    void 입력값을_검증한다() {
        int thisYear = Year.now().getValue();
        assertThrows(BoardException.class, () -> service.createPet(pet("", "개", null, null), null, 1L));
        assertThrows(BoardException.class, () -> service.createPet(pet("보리", "햄스터", null, null), null, 1L));
        assertThrows(BoardException.class, () -> service.createPet(pet("보리", "개", thisYear + 1, null), null, 1L));
        assertThrows(BoardException.class, () -> service.createPet(pet("보리", "개", null, new BigDecimal("0")), null, 1L));
        assertThrows(BoardException.class, () -> service.createPet(pet("보리", "개", null, new BigDecimal("3.255")), null, 1L));
        verify(petRepository, never()).save(any());
    }

    private static MyPetRequestDto pet(String name, String species, Integer birthYear, BigDecimal weight) {
        MyPetRequestDto dto = new MyPetRequestDto();
        dto.setName(name);
        dto.setSpecies(species);
        dto.setBirthYear(birthYear);
        dto.setWeightKg(weight);
        return dto;
    }
}
