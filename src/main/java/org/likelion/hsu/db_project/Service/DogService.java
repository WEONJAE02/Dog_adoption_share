package org.likelion.hsu.db_project.Service;

import lombok.RequiredArgsConstructor;
import org.likelion.hsu.db_project.Dto.DogRequestDto;
import org.likelion.hsu.db_project.Dto.DogResponseDto;
import org.likelion.hsu.db_project.Entity.Dog;
import org.likelion.hsu.db_project.Entity.DogDetail;
import org.likelion.hsu.db_project.Entity.DogImage;
import org.likelion.hsu.db_project.Repository.DogDetailRepository;
import org.likelion.hsu.db_project.Repository.DogImageRepository;

import org.likelion.hsu.db_project.Repository.DogRepository;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

    public DogResponseDto createDog(DogRequestDto dto, MultipartFile imageFile) throws IOException {
        String fileName = (imageFile!=null && !imageFile.isEmpty()) ? saveImage(imageFile) : null;

        Dog dog = Dog.builder()
                .name(dto.getName())
                .breed(dto.getBreed())
                .foundPlace(dto.getFoundPlace())
                .age(dto.getAge())
                .status(dto.getStatus())
                .memo(dto.getMemo())
                .gender(dto.getGender())
                .imageUrl(fileName != null ? "/uploads/" + fileName : null) // 프론트에서 바로 쓸 수 있게
                .build();

        dogRepository.save(dog);
        Long id = dogRepository.getLastInsertId();
        return getDogById(id);
    }

    public List<DogResponseDto> getAllDogs(String search, String status, String sort){
        return dogRepository.findAllWithConditions(search, status, sort)
                .stream().map(DogResponseDto::from).toList();
    }

    public DogResponseDto getDogById(Long id){
        Dog d = dogRepository.findById(id).orElseThrow(() -> new RuntimeException("데이터 없음"));
        return DogResponseDto.from(d);
    }

    public DogResponseDto updateDog(Long id, DogRequestDto dto, MultipartFile imageFile) throws IOException {
        Dog dog = dogRepository.findById(id).orElseThrow(() -> new RuntimeException("데이터 없음"));

        if (imageFile!=null && !imageFile.isEmpty()){
            String fileName = saveImage(imageFile);
            dog.setImageUrl("/uploads/" + fileName);
        }
        dog.setName(dto.getName());
        dog.setBreed(dto.getBreed());
        dog.setFoundPlace(dto.getFoundPlace());
        dog.setAge(dto.getAge());
        dog.setStatus(dto.getStatus());
        dog.setMemo(dto.getMemo());
        dog.setGender(dto.getGender());

        dogRepository.update(id, dog);
        return getDogById(id);
    }

    public void deleteDog(Long id){ dogRepository.delete(id); }

    private String saveImage(MultipartFile file) throws IOException {
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String unique = java.util.UUID.randomUUID() + ext;
        Path path = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
        java.nio.file.Files.createDirectories(path);
        java.nio.file.Files.copy(file.getInputStream(), path.resolve(unique), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return unique;
    }

    public Map<String,Object> getStatistics(){
        Map<String,Object> m = new java.util.HashMap<>();
        m.put("statusStats", dogRepository.getStatusStats());
        m.put("breedStats",  dogRepository.getBreedStats());
        return m;
    }
}
