package tech.muiru.ncba.model.request;

import jakarta.validation.constraints.NotBlank;

public record CountryReq(@NotBlank(message = "Country name must not be blank") String name) {
}
