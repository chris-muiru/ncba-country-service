package tech.muiru.ncba.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CountryInfoDTO(
        Long id,
        UUID countryUUID,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyIsoCode,
        String countryFlag,
        String isoCode,
        List<LanguageDTO> languages
) {
}
