package tech.muiru.ncba.dto;

import lombok.Builder;

@Builder
public record LanguageDTO(String isoCode, String name) {
}
