package tech.muiru.ncba.country.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tech.muiru.ncba.country.service.CountryService;
import tech.muiru.ncba.dto.CountryInfoDTO;
import tech.muiru.ncba.model.request.CountryReq;
import tech.muiru.ncba.model.response.CountryInfoRes;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/country")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CountryInfoRes fetchAndSave(@Valid @RequestBody CountryReq request) {
        log.info("POST /api/country - name: {}", request.name());
        CountryInfoDTO dto = countryService.fetchAndSave(request.name());
        return toRes(dto);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CountryInfoRes> getAll() {
        log.info("GET /api/country");
        return countryService.getAll().stream().map(this::toRes).toList();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CountryInfoRes getById(@PathVariable Long id) {
        log.info("GET /api/country/{}", id);
        return toRes(countryService.getById(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CountryInfoRes update(@PathVariable Long id, @RequestBody CountryInfoDTO dto) {
        log.info("PUT /api/country/{}", id);
        return toRes(countryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        log.info("DELETE /api/country/{}", id);
        countryService.delete(id);
    }

    private CountryInfoRes toRes(CountryInfoDTO dto) {
        return CountryInfoRes.builder()
                .id(dto.id())
                .countryUUID(dto.countryUUID())
                .name(dto.name())
                .capitalCity(dto.capitalCity())
                .phoneCode(dto.phoneCode())
                .continentCode(dto.continentCode())
                .currencyIsoCode(dto.currencyIsoCode())
                .countryFlag(dto.countryFlag())
                .isoCode(dto.isoCode())
                .languages(dto.languages())
                .build();
    }
}
