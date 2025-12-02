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

    // ✅ 등록 (여러 장 이미지 업로드)
    public DogResponseDto createDog(DogRequestDto dto, List<MultipartFile> images) throws IOException {

        // 대표 이미지 파일명 (images 중 첫 번째 이미지 사용)
        String mainImageName = null;

        if (images != null && !images.isEmpty()) {
            MultipartFile first = images.get(0);
            if (!first.isEmpty()) {
                mainImageName = saveImage(first);
            }
        }

        Dog dog = Dog.builder()
                .name(dto.getName())
                .breed(dto.getBreed())
                .age(dto.getAge())
                .status(dto.getStatus())
                .gender(dto.getGender())
                .imageName(mainImageName)  // ⭐ 대표 이미지로 설정
                .regDate(LocalDateTime.now())
                .build();

        // 1. dog 저장
        dogRepository.save(dog);
        Long dogId = dogRepository.getLastInsertId();

        // 2. 상세 정보 저장
        DogDetail detail = DogDetail.builder()
                .foundPlace(dto.getFoundPlace())
                .memo(dto.getMemo())
                .build();
        dogDetailRepository.save(detail, dogId);

        // 3. 전체 이미지 저장
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;

                String fileName = saveImage(file);

                DogImage dogImage = DogImage.builder()
                        .fileName(fileName)
                        .filePath(uploadDir + "/" + fileName)
                        .uploadDate(LocalDateTime.now())
                        .build();

                dogImageRepository.save(dogImage, dogId);
            }
        }

        return getDogById(dogId);
    }

    // ✅ 전체 조회 (검색 + 정렬 + 필터)
    public List<DogResponseDto> getAllDogs(String search, String status, String sort) {
        return dogRepository.findAllWithConditions(search, status, sort).stream()
                .map(d -> getDogById(d.getDogId()))
                .collect(Collectors.toList());
    }

    // ✅ 상세 조회
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
                .gender(dog.getGender())
                .foundPlace(detail != null ? detail.getFoundPlace() : null)
                .memo(detail != null ? detail.getMemo() : null)
                .imageNames(images.stream().map(DogImage::getFileName).collect(Collectors.toList()))
                .regDate(dog.getRegDate())
                .build();
    }

    // ✅ 수정 (여러 장 이미지 업로드)
    public DogResponseDto updateDog(Long id, DogRequestDto dto, List<MultipartFile> images) throws IOException {

        Dog existingDog = dogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 유기견 정보를 찾을 수 없습니다."));

        // 기존 이미지 전체 삭제
        dogImageRepository.deleteByDogId(id);

        // 대표 이미지명 기본값 = 기존 대표 이미지 (새 이미지 없을 때 대비)
        String mainImageName = existingDog.getImageName();

        // 대표 이미지 교체 + 새 이미지 전체 저장
        if (images != null && !images.isEmpty()) {

            for (int i = 0; i < images.size(); i++) {

                MultipartFile file = images.get(i);
                if (file.isEmpty()) continue;

                String fileName = saveImage(file);

                // 첫 번째 이미지를 대표 이미지로 설정
                if (i == 0) {
                    mainImageName = fileName;
                }

                DogImage img = DogImage.builder()
                        .fileName(fileName)
                        .filePath(uploadDir + "/" + fileName)
                        .uploadDate(LocalDateTime.now())
                        .build();

                dogImageRepository.save(img, id);
            }
        }

        // 텍스트 필드 수정
        existingDog.setName(dto.getName());
        existingDog.setBreed(dto.getBreed());
        existingDog.setAge(dto.getAge());
        existingDog.setStatus(dto.getStatus());
        existingDog.setGender(dto.getGender());
        existingDog.setImageName(mainImageName);

        dogRepository.update(id, existingDog);

        // 상세 정보 수정
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

    // ✅ 이미지 저장 (재사용)
    private String saveImage(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf("."));
        String uniqueName = UUID.randomUUID() + ext;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Files.copy(
                file.getInputStream(),
                uploadPath.resolve(uniqueName),
                StandardCopyOption.REPLACE_EXISTING
        );

        return uniqueName;
    }

    // ✅ 통계 (대시보드용)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("statusStats", dogRepository.getStatusStats());
        stats.put("breedStats", dogRepository.getBreedStats());
        return stats;
    }
}
