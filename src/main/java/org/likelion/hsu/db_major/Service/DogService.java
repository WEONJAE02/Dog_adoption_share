package org.likelion.hsu.db_major.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_major.Dto.*;
import org.likelion.hsu.db_major.Entity.*;
import org.likelion.hsu.db_major.Repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DogService {

    private final DogRepository dogRepository;
    private final DogDetailRepository dogDetailRepository;
    private final DogImageRepository dogImageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ✅ 등록
    public DogResponseDto createDog(DogRequestDto dto, MultipartFile imageFile) throws IOException {
        String fileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            fileName = saveImage(imageFile);
        }

        Dog dog = Dog.builder()
                .name(dto.getName())
                .breed(dto.getBreed())
                .age(dto.getAge())
                .status(dto.getStatus())
                .imageName(fileName)
                .regDate(LocalDateTime.now())
                .build();

        // 1. dog 테이블 저장
        dogRepository.save(dog);
        Long dogId = dogRepository.getLastInsertId();

        // 2. 상세 정보 저장
        DogDetail detail = DogDetail.builder()
                .foundPlace(dto.getFoundPlace())
                .memo(dto.getMemo())
                .build();
        dogDetailRepository.save(detail, dogId);

        // 3. 이미지 저장
        if (imageFile != null && !imageFile.isEmpty()) {
            DogImage image = DogImage.builder()
                    .fileName(fileName)
                    .filePath(uploadDir + "/" + fileName)
                    .uploadDate(LocalDateTime.now())
                    .build();
            dogImageRepository.save(image, dogId);
        }

        return getDogById(dogId);
    }

    // ✅ 전체 조회 (검색 + 정렬 + 필터)
    public List<DogResponseDto> getAllDogs(String search, String status, String sort) {
        return dogRepository.findAllWithConditions(search, status, sort).stream()
                .map(dog -> getDogById(dog.getDogId()))
                .collect(Collectors.toList());
    }

    // ✅ 단건 조회
    public DogResponseDto getDogById(Long id) {
        Dog dog = dogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 유기견 정보를 찾을 수 없습니다."));

        DogDetail detail = dogDetailRepository.findByDogId(id);
        List<DogImage> images = dogImageRepository.findByDogId(id);

        return DogResponseDto.builder()
                .dogId(dog.getDogId())
                .name(dog.getName())
                .breed(dog.getBreed())
                .age(dog.getAge())
                .status(dog.getStatus())
                .foundPlace(detail != null ? detail.getFoundPlace() : null)
                .memo(detail != null ? detail.getMemo() : null)
                .imageNames(images.stream().map(DogImage::getFileName).collect(Collectors.toList()))
                .regDate(dog.getRegDate())
                .build();
    }

    // ✅ 수정
    public DogResponseDto updateDog(Long id, DogRequestDto dto, MultipartFile imageFile) throws IOException {
        Dog existingDog = dogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 유기견 정보를 찾을 수 없습니다."));

        String fileName = existingDog.getImageName();
        if (imageFile != null && !imageFile.isEmpty()) {
            fileName = saveImage(imageFile);
            DogImage image = DogImage.builder()
                    .fileName(fileName)
                    .filePath(uploadDir + "/" + fileName)
                    .uploadDate(LocalDateTime.now())
                    .build();
            dogImageRepository.save(image, id);
        }

        existingDog.setName(dto.getName());
        existingDog.setBreed(dto.getBreed());
        existingDog.setAge(dto.getAge());
        existingDog.setStatus(dto.getStatus());
        existingDog.setImageName(fileName);
        dogRepository.update(id, existingDog);

        DogDetail detail = DogDetail.builder()
                .foundPlace(dto.getFoundPlace())
                .memo(dto.getMemo())
                .build();
        dogDetailRepository.update(id, detail);

        return getDogById(id);
    }

    // ✅ 삭제
    public void deleteDog(Long id) {
        dogImageRepository.deleteByDogId(id);
        dogDetailRepository.deleteByDogId(id);
        dogRepository.delete(id);
    }

    // ✅ 이미지 저장
    private String saveImage(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path destination = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    // ✅ 통계 (대시보드용)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("statusStats", dogRepository.getStatusStats());
        stats.put("breedStats", dogRepository.getBreedStats());
        return stats;
    }
}
