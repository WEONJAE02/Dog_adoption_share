package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.MyPetRequestDto;
import org.likelion.hsu.db_project.Dto.MyPetResponseDto;
import org.likelion.hsu.db_project.Entity.MyPet;
import org.likelion.hsu.db_project.Repository.MyPetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

/**
 * 내 반려동물 (건강체크 대상). 보호자 본인만 보고 고칠 수 있다.
 */
@Service
@RequiredArgsConstructor
public class MyPetService {

    public static final List<String> SPECIES = List.of("개", "고양이");
    public static final List<String> GENDERS = List.of("수컷", "암컷");

    private static final int MAX_PETS = 10;
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("150");

    private final MyPetRepository petRepository;
    private final ImageStorage imageStorage;
    private final HealthCheckService healthCheckService;

    public List<MyPetResponseDto> getPets(Long userId) {
        return petRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public MyPetResponseDto getPet(Long petId, Long userId) {
        return toDto(findMyPet(petId, userId));
    }

    // ✅ 등록 (사진 1장, 선택)
    @Transactional
    public MyPetResponseDto createPet(MyPetRequestDto dto, MultipartFile image, Long userId) throws IOException {
        if (petRepository.countByUserId(userId) >= MAX_PETS) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "반려동물은 최대 " + MAX_PETS + "마리까지 등록할 수 있습니다.");
        }
        MyPet pet = validate(dto);
        pet.setUserId(userId);
        pet.setImageName(storeImage(image));
        return toDto(findMyPet(petRepository.save(pet), userId));
    }

    // ✅ 수정 (새 사진을 올리면 교체, removeImage 면 삭제)
    @Transactional
    public MyPetResponseDto updatePet(Long petId, MyPetRequestDto dto, MultipartFile image, Long userId) throws IOException {
        MyPet existing = findMyPet(petId, userId);
        MyPet pet = validate(dto);

        String oldImage = existing.getImageName();
        String newImage = storeImage(image);
        if (newImage != null) {
            pet.setImageName(newImage);
        } else {
            pet.setImageName(dto.isRemoveImage() ? null : oldImage);
        }

        petRepository.update(petId, pet);
        if (oldImage != null && !oldImage.equals(pet.getImageName())) {
            imageStorage.delete(oldImage);
        }
        return toDto(findMyPet(petId, userId));
    }

    // ✅ 삭제 (사진, 건강체크 기록 포함)
    @Transactional
    public void deletePet(Long petId, Long userId) {
        MyPet pet = findMyPet(petId, userId);
        healthCheckService.deleteAllByPet(petId);
        petRepository.delete(petId);
        if (pet.getImageName() != null) {
            imageStorage.delete(pet.getImageName());
        }
    }

    // ✅ 회원 탈퇴 시 전부 삭제
    @Transactional
    public void deleteAllByUser(Long userId) {
        for (MyPet pet : petRepository.findByUserId(userId)) {
            deletePet(pet.getPetId(), userId);
        }
    }

    // 다른 사람의 반려동물은 있는지조차 알려주지 않는다
    private MyPet findMyPet(Long petId, Long userId) {
        return petRepository.findById(petId)
                .filter(pet -> pet.getUserId().equals(userId))
                .orElseThrow(() -> new BoardException(HttpStatus.NOT_FOUND, "반려동물 정보를 찾을 수 없습니다."));
    }

    private String storeImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (!imageStorage.isImage(image)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "이미지 파일(jpg, png, gif, webp)만 올릴 수 있습니다.");
        }
        return imageStorage.store(image);
    }

    // ✅ 입력값 검증 (선택 항목은 비워 두면 null)
    private MyPet validate(MyPetRequestDto dto) {
        String name = trim(dto.getName());
        String species = trim(dto.getSpecies());
        String breed = trim(dto.getBreed());
        String gender = trim(dto.getGender());
        String notes = trim(dto.getNotes());
        Integer birthYear = dto.getBirthYear();
        BigDecimal weight = dto.getWeightKg();
        int thisYear = Year.now().getValue();

        if (name.isEmpty() || name.length() > 20) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "이름은 1~20자로 입력해주세요.");
        }
        if (!SPECIES.contains(species)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "종류(개/고양이)를 선택해주세요.");
        }
        if (breed.length() > 30) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "품종은 30자 이내로 입력해주세요.");
        }
        if (!gender.isEmpty() && !GENDERS.contains(gender)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "성별이 올바르지 않습니다.");
        }
        if (birthYear != null && (birthYear < thisYear - 40 || birthYear > thisYear)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "태어난 해가 올바르지 않습니다.");
        }
        if (weight != null && (weight.signum() <= 0 || weight.compareTo(MAX_WEIGHT) > 0 || weight.scale() > 2)) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "몸무게는 0kg보다 크고 150kg 이하로, 소수점 둘째 자리까지 입력해주세요.");
        }
        if (notes.length() > 500) {
            throw new BoardException(HttpStatus.BAD_REQUEST, "특이사항은 500자 이내로 입력해주세요.");
        }

        return MyPet.builder()
                .name(name)
                .species(species)
                .breed(breed.isEmpty() ? null : breed)
                .gender(gender.isEmpty() ? null : gender)
                .neutered(dto.getNeutered())
                .birthYear(birthYear)
                .weightKg(weight)
                .notes(notes.isEmpty() ? null : notes)
                .build();
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    private MyPetResponseDto toDto(MyPet pet) {
        return MyPetResponseDto.builder()
                .petId(pet.getPetId())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .gender(pet.getGender())
                .neutered(pet.getNeutered())
                .birthYear(pet.getBirthYear())
                .weightKg(pet.getWeightKg())
                .notes(pet.getNotes())
                .imageName(pet.getImageName())
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .build();
    }
}
