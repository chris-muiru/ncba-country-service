package tech.muiru.ncba.model.response;

import lombok.Builder;
import tech.muiru.ncba.dto.LanguageDTO;

import java.util.List;
import java.util.UUID;

@Builder
public record CountryInfoRes(
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
