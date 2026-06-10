package tech.muiru.ncba.country.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.muiru.ncba.country.service.CountryService;
import tech.muiru.ncba.dto.CountryInfoDTO;
import tech.muiru.ncba.dto.LanguageDTO;
import tech.muiru.ncba.exception.CustomException;
import tech.muiru.ncba.io.entity.CountryInfoDef;
import tech.muiru.ncba.io.entity.LanguageDef;
import tech.muiru.ncba.io.repo.CountryInfoRepo;
import tech.muiru.ncba.soap.client.CountryInfoSoapClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountryServiceImp implements CountryService {

    private final CountryInfoRepo countryInfoRepo;
    private final CountryInfoSoapClient soapClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CountryInfoDTO fetchAndSave(String countryName) {
        String sentenceCase = toSentenceCase(countryName);
        log.info("fetchAndSave - countryName: {}", sentenceCase);

        String isoCode = soapClient.getCountryISOCode(sentenceCase);

        if (countryInfoRepo.existsByIsoCode(isoCode)) {
            throw new CustomException("Country already exists: " + sentenceCase);
        }

        CountryInfoDTO soapDto = soapClient.getFullCountryInfo(isoCode);

        CountryInfoDef entity = toEntity(soapDto);
        CountryInfoDef saved = countryInfoRepo.save(entity);

        log.info("fetchAndSave - saved country id: {}, name: {}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    public List<CountryInfoDTO> getAll() {
        log.info("getAll - fetching all countries");
        return countryInfoRepo.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public CountryInfoDTO getById(Long id) {
        log.info("getById - id: {}", id);
        CountryInfoDef entity = countryInfoRepo.findById(id)
                .orElseThrow(() -> new CustomException("Country not found with id: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CountryInfoDTO update(Long id, CountryInfoDTO dto) {
        log.info("update - id: {}", id);
        CountryInfoDef entity = countryInfoRepo.findById(id)
                .orElseThrow(() -> new CustomException("Country not found with id: " + id));

        entity.setName(dto.name());
        entity.setCapitalCity(dto.capitalCity());
        entity.setPhoneCode(dto.phoneCode());
        entity.setContinentCode(dto.continentCode());
        entity.setCurrencyIsoCode(dto.currencyIsoCode());
        entity.setCountryFlag(dto.countryFlag());
        entity.setIsoCode(dto.isoCode());

        if (dto.languages() != null) {
            entity.getLanguages().clear();
            dto.languages().forEach(langDto -> {
                LanguageDef lang = new LanguageDef();
                lang.setIsoCode(langDto.isoCode());
                lang.setName(langDto.name());
                lang.setCountryInfo(entity);
                entity.getLanguages().add(lang);
            });
        }

        CountryInfoDef updated = countryInfoRepo.save(entity);
        log.info("update - updated country id: {}", updated.getId());
        return toDto(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("delete - id: {}", id);
        CountryInfoDef entity = countryInfoRepo.findById(id)
                .orElseThrow(() -> new CustomException("Country not found with id: " + id));
        countryInfoRepo.delete(entity);
        log.info("delete - deleted country id: {}", id);
    }

    private String toSentenceCase(String name) {
        if (name == null || name.isBlank()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }

    private CountryInfoDef toEntity(CountryInfoDTO dto) {
        CountryInfoDef entity = new CountryInfoDef();
        entity.setName(dto.name());
        entity.setCapitalCity(dto.capitalCity());
        entity.setPhoneCode(dto.phoneCode());
        entity.setContinentCode(dto.continentCode());
        entity.setCurrencyIsoCode(dto.currencyIsoCode());
        entity.setCountryFlag(dto.countryFlag());
        entity.setIsoCode(dto.isoCode());

        if (dto.languages() != null) {
            dto.languages().forEach(langDto -> {
                LanguageDef lang = new LanguageDef();
                lang.setIsoCode(langDto.isoCode());
                lang.setName(langDto.name());
                lang.setCountryInfo(entity);
                entity.getLanguages().add(lang);
            });
        }
        return entity;
    }

    private CountryInfoDTO toDto(CountryInfoDef entity) {
        List<LanguageDTO> languages = entity.getLanguages().stream()
                .map(lang -> LanguageDTO.builder()
                        .isoCode(lang.getIsoCode())
                        .name(lang.getName())
                        .build())
                .toList();

        return CountryInfoDTO.builder()
                .id(entity.getId())
                .countryUUID(entity.getCountryUUID())
                .name(entity.getName())
                .capitalCity(entity.getCapitalCity())
                .phoneCode(entity.getPhoneCode())
                .continentCode(entity.getContinentCode())
                .currencyIsoCode(entity.getCurrencyIsoCode())
                .countryFlag(entity.getCountryFlag())
                .isoCode(entity.getIsoCode())
                .languages(languages)
                .build();
    }
}
